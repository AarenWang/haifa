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
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateFactory;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateStrategies;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class GraphResearchRuntime {

    private static final String GRAPH_NAME = "haifa-active-research-preflight";
    private static final String CREATE_OR_LOAD_PLAN = "create_or_load_plan";
    private static final String SEARCH_SOURCES = "search_sources";
    private static final String FETCH_SOURCES = "fetch_sources";
    private static final String EXTRACT_EVIDENCE = "extract_evidence";
    private static final String QUALITY_GATE = "quality_gate";
    private static final String PREPARE_SUBAGENT_SCOPE = "prepare_subagent_scope";
    private static final String DELEGATE_AGENT_LOOP = "delegate_agent_loop";

    private final DeerFlowProperties properties;
    private final GraphCheckpointRecorder checkpointRecorder;
    private final AgentGraphStateFactory stateFactory;

    public GraphResearchRuntime() {
        this(new DeerFlowProperties(), null, new AgentGraphStateFactory());
    }

    @Autowired
    public GraphResearchRuntime(DeerFlowProperties properties, GraphCheckpointRecorder checkpointRecorder) {
        this(properties, checkpointRecorder, new AgentGraphStateFactory());
    }

    GraphResearchRuntime(DeerFlowProperties properties, GraphCheckpointRecorder checkpointRecorder,
            AgentGraphStateFactory stateFactory) {
        this.properties = properties == null ? new DeerFlowProperties() : properties;
        this.checkpointRecorder = checkpointRecorder;
        this.stateFactory = stateFactory;
    }

    public Flux<AgentEvent> run(GraphResearchRuntimeRequest request) {
        return preflight(request)
                .thenMany(request.agentLoop().run(
                        request.loopConfig(),
                        request.runConfig(),
                        request.systemPrompt(),
                        request.userPrompt(),
                        request.eventSequence(),
                        request.toolPolicyService(),
                        request.activeSkills(),
                        request.uploadedFileIds()
                ));
    }

    private Mono<Void> preflight(GraphResearchRuntimeRequest request) {
        return Mono.fromRunnable(() -> {
            try {
                Map<String, Object> initialState = stateFactory.create(
                        request.runConfig(),
                        request.agentRequest(),
                        request.threadHistory(),
                        new ModelPrompt(request.systemPrompt(), request.userPrompt(), request.runConfig().modelName())
                );
                MemorySaver saver = checkpointEnabled() ? new MemorySaver() : null;
                CompiledGraph graph = saver == null ? researchGraph().compile() : researchGraph().compile(
                        CompileConfig.builder()
                                .saverConfig(SaverConfig.builder().register(saver).build())
                                .build());
                RunnableConfig runnableConfig = RunnableConfig.builder()
                        .threadId(request.runConfig().threadId())
                        .build();
                graph.stream(initialState, runnableConfig)
                        .collectList()
                        .block(Duration.ofSeconds(5));
                if (saver != null && checkpointRecorder != null) {
                    checkpointRecorder.record(GRAPH_NAME, request.runConfig(), runnableConfig, saver);
                }
            }
            catch (Exception ex) {
                throw new IllegalStateException("Graph active research preflight failed", ex);
            }
        });
    }

    private StateGraph researchGraph() throws Exception {
        StateGraph graph = new StateGraph(GRAPH_NAME, AgentGraphStateStrategies.keyStrategyFactory());
        graph.addNode(CREATE_OR_LOAD_PLAN, state -> completed(CREATE_OR_LOAD_PLAN, "planning"));
        graph.addNode(SEARCH_SOURCES, state -> completed(SEARCH_SOURCES, "search"));
        graph.addNode(FETCH_SOURCES, state -> completed(FETCH_SOURCES, "fetch"));
        graph.addNode(EXTRACT_EVIDENCE, state -> completed(EXTRACT_EVIDENCE, "evidence"));
        graph.addNode(QUALITY_GATE, state -> completed(QUALITY_GATE, "quality_gate"));
        graph.addNode(PREPARE_SUBAGENT_SCOPE, state -> CompletableFuture.completedFuture(Map.of(
                AgentGraphStateKeys.RESEARCH_PHASE, "subagent_scope",
                AgentGraphStateKeys.SUBAGENTS, Map.of(
                        "parentPolicyInherited", true,
                        "taskToolDelegatedToLegacyRuntime", true
                ),
                AgentGraphStateKeys.MODEL_STEPS, List.of(step(PREPARE_SUBAGENT_SCOPE, "prepared"))
        )));
        graph.addNode(DELEGATE_AGENT_LOOP, state -> completed(DELEGATE_AGENT_LOOP, "delegated"));
        graph.addEdge(StateGraph.START, CREATE_OR_LOAD_PLAN)
                .addEdge(CREATE_OR_LOAD_PLAN, SEARCH_SOURCES)
                .addEdge(SEARCH_SOURCES, FETCH_SOURCES)
                .addEdge(FETCH_SOURCES, EXTRACT_EVIDENCE)
                .addEdge(EXTRACT_EVIDENCE, QUALITY_GATE)
                .addEdge(QUALITY_GATE, PREPARE_SUBAGENT_SCOPE)
                .addEdge(PREPARE_SUBAGENT_SCOPE, DELEGATE_AGENT_LOOP)
                .addEdge(DELEGATE_AGENT_LOOP, StateGraph.END);
        return graph;
    }

    private CompletableFuture<Map<String, Object>> completed(String node, String phase) {
        return CompletableFuture.completedFuture(Map.of(
                AgentGraphStateKeys.RESEARCH_PHASE, phase,
                AgentGraphStateKeys.MODEL_STEPS, List.of(step(node, "completed"))
        ));
    }

    private static Map<String, Object> step(String node, String status) {
        return Map.of("node", node, "status", status);
    }

    private boolean checkpointEnabled() {
        return properties.getGraph() != null
                && properties.getGraph().getCheckpoint() != null
                && properties.getGraph().getCheckpoint().isEnabled();
    }
}
