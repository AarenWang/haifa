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
public class GraphChatRuntime {

    private static final String GRAPH_NAME = "haifa-active-chat-preflight";
    private static final String LOAD_CONTEXT = "load_context";
    private static final String APPLY_PROMPT_MIDDLEWARES = "apply_prompt_middlewares";
    private static final String DELEGATE_AGENT_LOOP = "delegate_agent_loop";

    private final AgentGraphStateFactory stateFactory;
    private final DeerFlowProperties properties;
    private final GraphCheckpointRecorder checkpointRecorder;

    public GraphChatRuntime() {
        this(new DeerFlowProperties(), null, new AgentGraphStateFactory());
    }

    @Autowired
    public GraphChatRuntime(DeerFlowProperties properties, GraphCheckpointRecorder checkpointRecorder) {
        this(properties, checkpointRecorder, new AgentGraphStateFactory());
    }

    GraphChatRuntime(DeerFlowProperties properties, GraphCheckpointRecorder checkpointRecorder,
            AgentGraphStateFactory stateFactory) {
        this.properties = properties == null ? new DeerFlowProperties() : properties;
        this.checkpointRecorder = checkpointRecorder;
        this.stateFactory = stateFactory;
    }

    public Flux<AgentEvent> run(GraphChatRuntimeRequest request) {
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

    private Mono<Void> preflight(GraphChatRuntimeRequest request) {
        return Mono.fromRunnable(() -> {
            try {
                Map<String, Object> initialState = stateFactory.create(
                        request.runConfig(),
                        request.agentRequest(),
                        request.threadHistory(),
                        new ModelPrompt(request.systemPrompt(), request.userPrompt(), request.runConfig().modelName())
                );
                MemorySaver saver = checkpointEnabled() ? new MemorySaver() : null;
                CompiledGraph graph = saver == null ? preflightGraph().compile() : preflightGraph().compile(
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
                throw new IllegalStateException("Graph active chat preflight failed", ex);
            }
        });
    }

    private StateGraph preflightGraph() throws Exception {
        StateGraph graph = new StateGraph(GRAPH_NAME, AgentGraphStateStrategies.keyStrategyFactory());
        graph.addNode(LOAD_CONTEXT, state -> CompletableFuture.completedFuture(Map.of(
                AgentGraphStateKeys.MODEL_STEPS, List.of(step(LOAD_CONTEXT, "completed"))
        )));
        graph.addNode(APPLY_PROMPT_MIDDLEWARES, state -> CompletableFuture.completedFuture(Map.of(
                AgentGraphStateKeys.MODEL_STEPS, List.of(step(APPLY_PROMPT_MIDDLEWARES, "completed"))
        )));
        graph.addNode(DELEGATE_AGENT_LOOP, state -> CompletableFuture.completedFuture(Map.of(
                AgentGraphStateKeys.MODEL_STEPS, List.of(step(DELEGATE_AGENT_LOOP, "delegated"))
        )));
        graph.addEdge(StateGraph.START, LOAD_CONTEXT)
                .addEdge(LOAD_CONTEXT, APPLY_PROMPT_MIDDLEWARES)
                .addEdge(APPLY_PROMPT_MIDDLEWARES, DELEGATE_AGENT_LOOP)
                .addEdge(DELEGATE_AGENT_LOOP, StateGraph.END);
        return graph;
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
