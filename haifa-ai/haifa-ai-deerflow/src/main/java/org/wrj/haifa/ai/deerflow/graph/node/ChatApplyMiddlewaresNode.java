package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;

@Component
public class ChatApplyMiddlewaresNode implements AsyncNodeAction {

    @org.springframework.beans.factory.annotation.Autowired
    private GraphExecutionManager graphExecutionManager;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.wrj.haifa.ai.deerflow.config.DeerFlowProperties properties;

    @org.springframework.beans.factory.annotation.Autowired
    private List<org.wrj.haifa.ai.deerflow.middleware.AgentMiddleware> middlewares;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            AgentGraphStateView view = AgentGraphStateView.of(state);
            String runId = view.runId();
            String threadId = view.threadId();

            String modelName = view.modelName();
            if (modelName == null || modelName.isBlank()) {
                modelName = state.<String>value(AgentGraphStateKeys.MODEL_NAME).orElse("");
            }

            org.wrj.haifa.ai.deerflow.agent.AgentRunConfig runConfig = new org.wrj.haifa.ai.deerflow.agent.AgentRunConfig(
                    threadId,
                    runId,
                    modelName,
                    false, // thinkingEnabled
                    false, // planMode
                    10,    // maxIterations
                    java.nio.file.Path.of(""),
                    view.mode(),
                    org.wrj.haifa.ai.deerflow.agent.ResearchOptions.defaults(),
                    view.map(AgentGraphStateKeys.REQUEST_METADATA)
            );

            String userMessage = "";
            for (var msg : view.messageWindow()) {
                if ("USER".equals(msg.get("role"))) {
                    userMessage = (String) msg.get("content");
                    break;
                }
            }

            org.wrj.haifa.ai.deerflow.agent.AgentRequest agentRequest = new org.wrj.haifa.ai.deerflow.agent.AgentRequest(
                    threadId,
                    userMessage,
                    modelName,
                    List.of(),
                    view.mode(),
                    org.wrj.haifa.ai.deerflow.agent.ResearchOptions.defaults(),
                    "default-user",
                    view.map(AgentGraphStateKeys.REQUEST_METADATA)
            );

            List<org.wrj.haifa.ai.deerflow.tool.ToolResult> toolResults = new java.util.ArrayList<>();
            for (var tr : view.toolResults()) {
                String toolName = (String) tr.get("toolName");
                String content = (String) tr.get("content");
                Map<String, Object> metadata = (Map<String, Object>) tr.get("metadata");
                toolResults.add(new org.wrj.haifa.ai.deerflow.tool.ToolResult(toolName, content, metadata));
            }

            var activeSkills = view.activeSkills();

            org.wrj.haifa.ai.deerflow.middleware.AgentRuntimeContext runtimeContext =
                    new org.wrj.haifa.ai.deerflow.middleware.AgentRuntimeContext(
                            runConfig,
                            agentRequest,
                            toolResults,
                            properties,
                            activeSkills
                    );

            List<org.wrj.haifa.ai.deerflow.middleware.AgentMiddleware> sorted = middlewares.stream()
                    .sorted(java.util.Comparator.comparingInt(m -> {
                        org.wrj.haifa.ai.deerflow.middleware.MiddlewareOrder order = m.getClass().getAnnotation(org.wrj.haifa.ai.deerflow.middleware.MiddlewareOrder.class);
                        return order != null ? order.value() : 99;
                    }))
                    .toList();

            org.wrj.haifa.ai.deerflow.middleware.MiddlewareChain chain = new org.wrj.haifa.ai.deerflow.middleware.MiddlewareChain(sorted);
            long timeoutMs = properties == null ? 30_000L : properties.getModelTimeout();
            org.wrj.haifa.ai.deerflow.model.ModelPrompt prompt = chain.next(runtimeContext)
                    .block(java.time.Duration.ofMillis(timeoutMs));

            Map<String, Object> promptMap = new HashMap<>();
            if (prompt != null) {
                promptMap.put("systemPrompt", prompt.systemPrompt() == null ? "" : prompt.systemPrompt());
                promptMap.put("userPrompt", prompt.userPrompt() == null ? "" : prompt.userPrompt());
                promptMap.put("modelName", prompt.modelName() == null ? "" : prompt.modelName());
            }

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.MODEL_PROMPT, promptMap);
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "apply_prompt_middlewares", "status", "completed")));
            return update;
        }, executor);
    }
}
