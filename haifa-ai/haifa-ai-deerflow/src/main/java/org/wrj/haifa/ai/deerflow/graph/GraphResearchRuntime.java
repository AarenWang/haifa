package org.wrj.haifa.ai.deerflow.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.GraphCheckpointRecorder;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.SQLiteCheckpointSaver;
import org.wrj.haifa.ai.deerflow.graph.node.EvidenceExtractionNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchFetchNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchPlanningNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchQualityGateNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchReportNode;
import org.wrj.haifa.ai.deerflow.graph.node.ResearchSearchNode;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateFactory;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateStrategies;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class GraphResearchRuntime {

    private static final String GRAPH_NAME = ResearchAgentGraph.GRAPH_NAME;
    private static final String CREATE_OR_LOAD_PLAN = ResearchAgentGraph.CREATE_OR_LOAD_PLAN;
    private static final String SEARCH_SOURCES = ResearchAgentGraph.SEARCH_SOURCES;
    private static final String FETCH_SOURCES = ResearchAgentGraph.FETCH_SOURCES;
    private static final String EXTRACT_EVIDENCE = ResearchAgentGraph.EXTRACT_EVIDENCE;
    private static final String QUALITY_GATE = ResearchAgentGraph.QUALITY_GATE;
    private static final String WRITE_REPORT = ResearchAgentGraph.WRITE_REPORT;

    private final DeerFlowProperties properties;
    @SuppressWarnings("unused")
    private final GraphCheckpointRecorder checkpointRecorder;
    private final AgentGraphStateFactory stateFactory;
    private final SQLiteCheckpointSaver sqliteCheckpointSaver;
    private final ResearchAgentGraph researchAgentGraph;

    private final ResearchPlanningNode planningNode;
    private final ResearchSearchNode searchNode;
    private final ResearchFetchNode fetchNode;
    private final EvidenceExtractionNode evidenceNode;
    private final ResearchQualityGateNode qualityGateNode;
    private final ResearchReportNode reportNode;

    public GraphResearchRuntime() {
        this(new DeerFlowProperties(), null, new AgentGraphStateFactory(), null, null,
                null, null, null, null, null, null);
    }

    @Autowired
    public GraphResearchRuntime(DeerFlowProperties properties,
                                GraphCheckpointRecorder checkpointRecorder,
                                AgentGraphStateFactory stateFactory,
                                SQLiteCheckpointSaver sqliteCheckpointSaver,
                                ResearchAgentGraph researchAgentGraph,
                                ResearchPlanningNode planningNode,
                                ResearchSearchNode searchNode,
                                ResearchFetchNode fetchNode,
                                EvidenceExtractionNode evidenceNode,
                                ResearchQualityGateNode qualityGateNode,
                                ResearchReportNode reportNode) {
        this.properties = properties == null ? new DeerFlowProperties() : properties;
        this.checkpointRecorder = checkpointRecorder;
        this.stateFactory = stateFactory == null ? new AgentGraphStateFactory() : stateFactory;
        this.sqliteCheckpointSaver = sqliteCheckpointSaver;
        this.researchAgentGraph = researchAgentGraph;
        this.planningNode = planningNode;
        this.searchNode = searchNode;
        this.fetchNode = fetchNode;
        this.evidenceNode = evidenceNode;
        this.qualityGateNode = qualityGateNode;
        this.reportNode = reportNode;
    }

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    public Flux<AgentEvent> run(GraphResearchRuntimeRequest request) {
        Sinks.Many<AgentEvent> eventSink = Sinks.many().unicast().onBackpressureBuffer();
        String runId = request.runConfig().runId();
        GraphEventRegistry.register(runId, eventSink, request.eventSequence());

        java.util.concurrent.Executor executor = graphExecutionManager != null
                ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> initialState = new HashMap<>(stateFactory.create(
                        request.runConfig(),
                        request.agentRequest(),
                        request.threadHistory(),
                        new ModelPrompt(request.systemPrompt(), request.userPrompt(), request.runConfig().modelName()),
                        request.activeSkills()
                ));
                initialState.put(AgentGraphStateKeys.RESEARCH_STEPS, 0);
                initialState.put(AgentGraphStateKeys.QUALITY_GATE_PASSED, false);
                initialState.put(AgentGraphStateKeys.RESEARCH_GAPS, List.of());
                initialState.put(AgentGraphStateKeys.REPLAN_COUNT, 0);
                initialState.put(AgentGraphStateKeys.CITATION_VERIFICATION, Map.of());
                initialState.put(AgentGraphStateKeys.EMITTED_EVIDENCE_IDS, List.of());
                initialState.put(AgentGraphStateKeys.RESEARCH_SOURCE_COUNT, 0);
                initialState.put(AgentGraphStateKeys.RESEARCH_EVIDENCE_COUNT, 0);

                BaseCheckpointSaver saver = checkpointEnabled() ? sqliteCheckpointSaver : null;
                int maxIterations = request.loopConfig().maxSteps();
                StateGraph activeGraph = activeResearchGraph(maxIterations);
                CompiledGraph graph = saver == null ? activeGraph.compile() : activeGraph.compile(
                        CompileConfig.builder()
                                .recursionLimit(maxIterations * 7 + 8)
                                .saverConfig(SaverConfig.builder().register(saver).build())
                                .build());

                RunnableConfig runnableConfig = RunnableConfig.builder()
                        .threadId(request.runConfig().threadId())
                        .build();
                runnableConfig.context().put("runId", runId);
                runnableConfig.context().put("graphName", GRAPH_NAME);

                Optional<Checkpoint> resumeCheckpoint = Optional.empty();
                if (saver != null) {
                    resumeCheckpoint = saver.get(runnableConfig)
                            .filter(checkpoint -> canResumeResearch(checkpoint, runId));
                }

                RunnableConfig executionConfig = resumeCheckpoint
                        .map(checkpoint -> RunnableConfig.builder(runnableConfig)
                                .checkPointId(checkpoint.getId())
                                .nextNode(checkpoint.getNextNodeId())
                                .build())
                        .orElse(runnableConfig);
                executionConfig.context().put("runId", runId);
                executionConfig.context().put("graphName", GRAPH_NAME);

                var streamResult = resumeCheckpoint.isPresent()
                        ? graph.stream(Map.of(
                                AgentGraphStateKeys.RUN_ID, runId,
                                AgentGraphStateKeys.THREAD_ID, request.runConfig().threadId()
                        ), executionConfig)
                        : graph.stream(initialState, runnableConfig);

                streamResult.collectList().block(Duration.ofMillis(request.loopConfig().timeoutMs()));
                eventSink.tryEmitComplete();
            }
            catch (Exception ex) {
                eventSink.tryEmitError(ex);
            }
            finally {
                GraphEventRegistry.deregister(runId);
            }
        }, executor);

        return eventSink.asFlux();
    }

    private StateGraph activeResearchGraph(int maxSteps) throws Exception {
        if (researchAgentGraph != null) {
            return researchAgentGraph.build(maxSteps);
        }
        return deterministicResearchGraph(maxSteps);
    }

    private StateGraph deterministicResearchGraph(int maxSteps) throws Exception {
        StateGraph graph = new StateGraph(GRAPH_NAME, AgentGraphStateStrategies.keyStrategyFactory());
        graph.addNode(CREATE_OR_LOAD_PLAN, planningNode);
        graph.addNode(SEARCH_SOURCES, searchNode);
        graph.addNode(FETCH_SOURCES, fetchNode);
        graph.addNode(EXTRACT_EVIDENCE, evidenceNode);
        graph.addNode(QUALITY_GATE, qualityGateNode);
        graph.addNode(WRITE_REPORT, reportNode);

        graph.addEdge(StateGraph.START, CREATE_OR_LOAD_PLAN)
                .addEdge(CREATE_OR_LOAD_PLAN, SEARCH_SOURCES)
                .addEdge(SEARCH_SOURCES, FETCH_SOURCES)
                .addEdge(FETCH_SOURCES, EXTRACT_EVIDENCE)
                .addEdge(EXTRACT_EVIDENCE, QUALITY_GATE);

        graph.addConditionalEdges(QUALITY_GATE, state -> {
            Boolean passed = (Boolean) state.data().getOrDefault(AgentGraphStateKeys.QUALITY_GATE_PASSED, false);
            Integer steps = (Integer) state.data().getOrDefault(AgentGraphStateKeys.RESEARCH_STEPS, 0);
            if (Boolean.TRUE.equals(passed) || steps >= maxSteps) {
                return CompletableFuture.completedFuture(WRITE_REPORT);
            }
            return CompletableFuture.completedFuture(SEARCH_SOURCES);
        }, Map.of(WRITE_REPORT, WRITE_REPORT, SEARCH_SOURCES, SEARCH_SOURCES));

        graph.addEdge(WRITE_REPORT, StateGraph.END);
        return graph;
    }

    private boolean checkpointEnabled() {
        return properties.getGraph() != null
                && properties.getGraph().getCheckpoint() != null
                && properties.getGraph().getCheckpoint().isEnabled()
                && sqliteCheckpointSaver != null;
    }

    private static boolean canResumeResearch(Checkpoint checkpoint, String runId) {
        String nextNodeId = checkpoint.getNextNodeId();
        if (nextNodeId == null || nextNodeId.isBlank() || StateGraph.END.equals(nextNodeId)) {
            return false;
        }
        Object checkpointRunId = checkpoint.getState().get(AgentGraphStateKeys.RUN_ID);
        return checkpointRunId != null && String.valueOf(checkpointRunId).equals(runId);
    }
}
