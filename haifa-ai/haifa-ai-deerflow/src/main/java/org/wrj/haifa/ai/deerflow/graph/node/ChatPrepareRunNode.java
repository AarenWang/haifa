package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.RunExecutionContext;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.RunExecutionContextRegistry;
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

/** Executes side-effecting run preparation exactly once per graph run. */
@Component
public class ChatPrepareRunNode implements AsyncNodeAction {

    private final DeerFlowProperties properties;
    private final List<AgentMiddleware> runPreparationMiddlewares;
    private final GraphExecutionManager graphExecutionManager;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RunExecutionContextRegistry executionContextRegistry;

    public ChatPrepareRunNode(DeerFlowProperties properties, List<AgentMiddleware> middlewares,
            GraphExecutionManager graphExecutionManager) {
        this.properties = properties;
        this.runPreparationMiddlewares = MiddlewarePhases.select(middlewares, MiddlewarePhase.RUN_PREPARATION);
        this.graphExecutionManager = graphExecutionManager;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> prepare(state), graphExecutionManager.getExecutor());
    }

    private Map<String, Object> prepare(OverAllState state) {
        AgentGraphStateView view = AgentGraphStateView.of(state);
        boolean alreadyPrepared = state.<Boolean>value(AgentGraphStateKeys.RUN_PREPARED).orElse(false);
        Map<String, Object> existingBase = view.map(AgentGraphStateKeys.RUN_PROMPT_BASE);
        if (alreadyPrepared && !existingBase.isEmpty()) {
            return Map.of(AgentGraphStateKeys.MODEL_STEPS,
                    List.of(Map.of("node", "prepare_run", "status", "skipped", "reason", "already_prepared")));
        }

        RunExecutionContext lifecycle = requireLifecycle(view.runId());
        AgentRuntimeContext runtimeContext = AgentRuntimeContext.of(
                lifecycle.runConfig(),
                lifecycle.agentRequest(),
                List.of(),
                properties,
                lifecycle.activeSkills());

        ModelPrompt basePrompt = new MiddlewareChain(runPreparationMiddlewares)
                .next(runtimeContext)
                .block(java.time.Duration.ofMillis(properties.getModelTimeout()));
        if (basePrompt == null) {
            throw new IllegalStateException("Run preparation produced no base prompt");
        }

        Map<String, Object> update = new HashMap<>();
        update.put(AgentGraphStateKeys.RUN_PROMPT_BASE, GraphPromptStateMapper.toState(basePrompt));
        update.put(AgentGraphStateKeys.RUN_PREPARED, true);
        update.put(AgentGraphStateKeys.MODEL_STEPS,
                List.of(Map.of("node", "prepare_run", "status", "completed")));
        return update;
    }

    private RunExecutionContext requireLifecycle(String runId) {
        return executionContextRegistry == null ? missing(runId) : executionContextRegistry.get(runId)
                .orElseThrow(() -> new IllegalStateException("Graph lifecycle context missing for run " + runId));
    }

    private static RunExecutionContext missing(String runId) {
        throw new IllegalStateException("Graph execution context registry missing for run " + runId);
    }
}
