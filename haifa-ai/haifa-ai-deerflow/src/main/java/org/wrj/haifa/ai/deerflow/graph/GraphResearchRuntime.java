package org.wrj.haifa.ai.deerflow.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.GraphCheckpointRecorder;
import org.wrj.haifa.ai.deerflow.graph.node.*;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateFactory;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateStrategies;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class GraphResearchRuntime {

    private static final String GRAPH_NAME = "haifa-active-research";
    private static final String CREATE_OR_LOAD_PLAN = "create_or_load_plan";
    private static final String SEARCH_SOURCES = "search_sources";
    private static final String FETCH_SOURCES = "fetch_sources";
    private static final String EXTRACT_EVIDENCE = "extract_evidence";
    private static final String QUALITY_GATE = "quality_gate";
    private static final String WRITE_REPORT = "write_report";

    private final DeerFlowProperties properties;
    private final GraphCheckpointRecorder checkpointRecorder;
    private final AgentGraphStateFactory stateFactory;

    private final ResearchPlanningNode planningNode;
    private final ResearchSearchNode searchNode;
    private final ResearchFetchNode fetchNode;
    private final EvidenceExtractionNode evidenceNode;
    private final ResearchQualityGateNode qualityGateNode;
    private final ResearchReportNode reportNode;

    public GraphResearchRuntime() {
        this(new DeerFlowProperties(), null, new AgentGraphStateFactory(), null, null, null, null, null, null);
    }

    @Autowired
    public GraphResearchRuntime(DeerFlowProperties properties,
                                GraphCheckpointRecorder checkpointRecorder,
                                AgentGraphStateFactory stateFactory,
                                ResearchPlanningNode planningNode,
                                ResearchSearchNode searchNode,
                                ResearchFetchNode fetchNode,
                                EvidenceExtractionNode evidenceNode,
                                ResearchQualityGateNode qualityGateNode,
                                ResearchReportNode reportNode) {
        this.properties = properties == null ? new DeerFlowProperties() : properties;
        this.checkpointRecorder = checkpointRecorder;
        this.stateFactory = stateFactory == null ? new AgentGraphStateFactory() : stateFactory;
        this.planningNode = planningNode;
        this.searchNode = searchNode;
        this.fetchNode = fetchNode;
        this.evidenceNode = evidenceNode;
        this.qualityGateNode = qualityGateNode;
        this.reportNode = reportNode;
    }

    public Flux<AgentEvent> run(GraphResearchRuntimeRequest request) {
        Sinks.Many<AgentEvent> eventSink = Sinks.many().unicast().onBackpressureBuffer();
        String runId = request.runConfig().runId();
        GraphEventRegistry.register(runId, eventSink, request.eventSequence());

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> initialState = new HashMap<>(stateFactory.create(
                        request.runConfig(),
                        request.agentRequest(),
                        request.threadHistory(),
                        new ModelPrompt(request.systemPrompt(), request.userPrompt(), request.runConfig().modelName())
                ));
                // Track research steps inside state
                initialState.put("research_steps", 0);
                initialState.put("quality_gate_passed", false);
                initialState.put("emittedEvidenceIds", List.of());

                MemorySaver saver = checkpointEnabled() ? new MemorySaver() : null;
                int maxIterations = request.loopConfig().maxSteps();
                CompiledGraph graph = saver == null ? researchGraph(maxIterations).compile() : researchGraph(maxIterations).compile(
                        CompileConfig.builder()
                                .recursionLimit(maxIterations * 5 + 5)
                                .saverConfig(SaverConfig.builder().register(saver).build())
                                .build());

                RunnableConfig runnableConfig = RunnableConfig.builder()
                        .threadId(request.runConfig().threadId())
                        .build();

                graph.stream(initialState, runnableConfig)
                        .collectList()
                        .block(Duration.ofMillis(request.loopConfig().timeoutMs()));

                if (saver != null && checkpointRecorder != null) {
                    checkpointRecorder.record(GRAPH_NAME, request.runConfig(), runnableConfig, saver);
                }
                eventSink.tryEmitComplete();
            }
            catch (Exception ex) {
                eventSink.tryEmitError(ex);
            }
            finally {
                GraphEventRegistry.deregister(runId);
            }
        });

        return eventSink.asFlux();
    }

    private StateGraph researchGraph(int maxSteps) throws Exception {
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

        // Conditional edge from quality_gate
        graph.addConditionalEdges(QUALITY_GATE, state -> {
            Boolean passed = (Boolean) state.data().getOrDefault("quality_gate_passed", false);
            Integer steps = (Integer) state.data().getOrDefault("research_steps", 0);
            if (Boolean.TRUE.equals(passed) || steps >= maxSteps) {
                return CompletableFuture.completedFuture(WRITE_REPORT);
            } else {
                return CompletableFuture.completedFuture(SEARCH_SOURCES);
            }
        }, Map.of(WRITE_REPORT, WRITE_REPORT, SEARCH_SOURCES, SEARCH_SOURCES));

        graph.addEdge(WRITE_REPORT, StateGraph.END);

        return graph;
    }

    private boolean checkpointEnabled() {
        return properties.getGraph() != null
                && properties.getGraph().getCheckpoint() != null
                && properties.getGraph().getCheckpoint().isEnabled();
    }
}
