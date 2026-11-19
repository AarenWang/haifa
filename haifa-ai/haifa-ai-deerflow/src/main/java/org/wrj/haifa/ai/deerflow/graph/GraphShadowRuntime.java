package org.wrj.haifa.ai.deerflow.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateFactory;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateStrategies;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class GraphShadowRuntime {

    private static final String LOAD_CONTEXT = "load_context";
    private static final String APPLY_PROMPT_MIDDLEWARES = "apply_prompt_middlewares";
    private static final String CALL_MODEL = "call_model";
    private static final String PARSE_TOOL_CALLS = "parse_tool_calls";
    private static final String FINALIZE = "finalize";

    private final AgentGraphStateFactory stateFactory;

    public GraphShadowRuntime() {
        this(new AgentGraphStateFactory());
    }

    GraphShadowRuntime(AgentGraphStateFactory stateFactory) {
        this.stateFactory = stateFactory;
    }

    public Mono<AgentGraphShadowResult> run(AgentRunConfig config, AgentRequest request,
            List<MessageRecord> threadHistory, ModelPrompt prompt) {
        return Mono.fromCallable(() -> {
            long startedAt = System.currentTimeMillis();
            Map<String, Object> initialState = stateFactory.create(config, request, threadHistory, prompt);
            CompiledGraph graph = shadowGraph().compile();
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(config.threadId())
                    .build();
            List<NodeOutput> outputs = graph.stream(initialState, runnableConfig)
                    .collectList()
                    .block(Duration.ofSeconds(5));
            Map<String, Object> finalState = outputs == null || outputs.isEmpty()
                    ? initialState
                    : Map.copyOf(outputs.get(outputs.size() - 1).state().data());
            List<String> visitedNodes = outputs == null ? List.of() : outputs.stream()
                    .map(NodeOutput::node)
                    .filter(node -> !StateGraph.START.equals(node) && !StateGraph.END.equals(node))
                    .toList();
            return new AgentGraphShadowResult(config.runId(), config.threadId(), visitedNodes, finalState,
                    System.currentTimeMillis() - startedAt);
        });
    }

    private StateGraph shadowGraph() throws Exception {
        StateGraph graph = new StateGraph("haifa-chat-shadow", AgentGraphStateStrategies.keyStrategyFactory());
        graph.addNode(LOAD_CONTEXT, state -> CompletableFuture.completedFuture(Map.of(
                AgentGraphStateKeys.MODEL_STEPS, List.of(step(LOAD_CONTEXT, "completed"))
        )));
        graph.addNode(APPLY_PROMPT_MIDDLEWARES, state -> CompletableFuture.completedFuture(Map.of(
                AgentGraphStateKeys.MODEL_STEPS, List.of(step(APPLY_PROMPT_MIDDLEWARES, "completed"))
        )));
        graph.addNode(CALL_MODEL, state -> CompletableFuture.completedFuture(Map.of(
                AgentGraphStateKeys.MODEL_STEPS, List.of(step(CALL_MODEL, "shadow_skipped"))
        )));
        graph.addNode(PARSE_TOOL_CALLS, state -> CompletableFuture.completedFuture(Map.of(
                AgentGraphStateKeys.PENDING_TOOL_CALLS, List.of(),
                AgentGraphStateKeys.MODEL_STEPS, List.of(step(PARSE_TOOL_CALLS, "completed"))
        )));
        graph.addNode(FINALIZE, state -> CompletableFuture.completedFuture(Map.of(
                AgentGraphStateKeys.MODEL_STEPS, List.of(step(FINALIZE, "completed"))
        )));
        graph.addEdge(StateGraph.START, LOAD_CONTEXT)
                .addEdge(LOAD_CONTEXT, APPLY_PROMPT_MIDDLEWARES)
                .addEdge(APPLY_PROMPT_MIDDLEWARES, CALL_MODEL)
                .addEdge(CALL_MODEL, PARSE_TOOL_CALLS)
                .addEdge(PARSE_TOOL_CALLS, FINALIZE)
                .addEdge(FINALIZE, StateGraph.END);
        return graph;
    }

    private static Map<String, Object> step(String node, String status) {
        return Map.of("node", node, "status", status);
    }
}
