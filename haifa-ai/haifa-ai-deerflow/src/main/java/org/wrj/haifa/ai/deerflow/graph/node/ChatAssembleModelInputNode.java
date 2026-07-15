package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphChatLifecycleContext;
import org.wrj.haifa.ai.deerflow.graph.GraphChatLifecycleRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.graph.state.GraphPromptStateMapper;
import org.wrj.haifa.ai.deerflow.middleware.AgentMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.AgentRuntimeContext;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewareChain;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewarePhase;
import org.wrj.haifa.ai.deerflow.middleware.MiddlewarePhases;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

/** Derives a fresh model prompt from the stable run base before every model call. */
@Component
public class ChatAssembleModelInputNode implements AsyncNodeAction {

    private final DeerFlowProperties properties;
    private final List<AgentMiddleware> modelInputMiddlewares;
    private final GraphExecutionManager graphExecutionManager;

    public ChatAssembleModelInputNode(DeerFlowProperties properties, List<AgentMiddleware> middlewares,
            GraphExecutionManager graphExecutionManager) {
        this.properties = properties;
        this.modelInputMiddlewares = MiddlewarePhases.select(middlewares, MiddlewarePhase.MODEL_INPUT);
        this.graphExecutionManager = graphExecutionManager;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> assemble(state), graphExecutionManager.getExecutor());
    }

    private Map<String, Object> assemble(OverAllState state) {
        AgentGraphStateView view = AgentGraphStateView.of(state);
        GraphChatLifecycleContext lifecycle = GraphChatLifecycleRegistry.get(view.runId())
                .orElseThrow(() -> new IllegalStateException(
                        "Graph lifecycle context missing for run " + view.runId()));
        Map<String, Object> baseState = view.map(AgentGraphStateKeys.RUN_PROMPT_BASE);
        if (baseState.isEmpty()) {
            throw new IllegalStateException("Run prompt base is missing for run " + view.runId());
        }

        ModelPrompt basePrompt = GraphPromptStateMapper.fromState(baseState, lifecycle.runConfig().modelName());
        AgentRuntimeContext runtimeContext = AgentRuntimeContext.of(
                lifecycle.runConfig(),
                lifecycle.agentRequest(),
                toolResults(view.toolResults()),
                properties,
                lifecycle.activeSkills());

        ModelPrompt prompt = new MiddlewareChain(modelInputMiddlewares, basePrompt)
                .next(runtimeContext)
                .block(java.time.Duration.ofMillis(properties.getModelTimeout()));
        if (prompt == null) {
            throw new IllegalStateException("Model input assembly produced no prompt");
        }

        int revision = state.<Integer>value(AgentGraphStateKeys.PROMPT_REVISION).orElse(0) + 1;
        Map<String, Object> update = new HashMap<>();
        update.put(AgentGraphStateKeys.MODEL_PROMPT, GraphPromptStateMapper.toState(prompt));
        update.put(AgentGraphStateKeys.PROMPT_REVISION, revision);
        update.put(AgentGraphStateKeys.MODEL_STEPS,
                List.of(Map.of("node", "assemble_model_input", "status", "completed", "revision", revision)));
        return update;
    }

    private static List<ToolResult> toolResults(List<Map<String, Object>> stateResults) {
        List<ToolResult> results = new ArrayList<>();
        for (Map<String, Object> item : stateResults) {
            String toolName = item.get("toolName") instanceof String text ? text : "";
            String content = item.get("content") instanceof String text ? text : "";
            Map<String, Object> metadata = stringObjectMap(item.get("metadata"));
            results.add(new ToolResult(toolName, content, metadata));
        }
        return List.copyOf(results);
    }

    private static Map<String, Object> stringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> result = new HashMap<>();
        raw.forEach((key, item) -> {
            if (key instanceof String text) {
                result.put(text, item);
            }
        });
        return Map.copyOf(result);
    }
}
