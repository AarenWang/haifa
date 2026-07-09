package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyDecision;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ChatExecuteToolsNode implements AsyncNodeAction {

    private final ToolRegistry toolRegistry;
    private final DeerFlowProperties properties;
    private final ToolPolicyService toolPolicyService;

    @Autowired(required = false)
    private ToolCallStore toolCallStore;

    public ChatExecuteToolsNode(ToolRegistry toolRegistry, DeerFlowProperties properties) {
        this(toolRegistry, properties, null);
    }

    @Autowired
    public ChatExecuteToolsNode(ToolRegistry toolRegistry, DeerFlowProperties properties,
                                ToolPolicyService toolPolicyService) {
        this.toolRegistry = toolRegistry;
        this.properties = properties;
        this.toolPolicyService = toolPolicyService;
    }

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");

            AgentGraphStateView view = AgentGraphStateView.of(state);
            var activeSkills = view.activeSkills();
            List<Map<String, Object>> pending = view.listOfMaps(AgentGraphStateKeys.PENDING_TOOL_CALLS);
            List<String> uploadedFileIds = view.list(AgentGraphStateKeys.UPLOADED_FILE_IDS).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
            List<Map<String, Object>> toolMessages = new ArrayList<>();
            List<Map<String, Object>> toolResultsList = new ArrayList<>();

            for (Map<String, Object> call : pending) {
                String callId = (String) call.get("id");
                String name = (String) call.get("name");
                String arguments = (String) call.get("arguments");
                String safeName = name == null ? "" : name;
                String safeArguments = arguments == null ? "" : arguments;

                persistToolCallRequested(callId, safeName, safeArguments, runId, threadId);

                long startTime = System.currentTimeMillis();
                String status = "SUCCESS";
                String content;
                String errorType = "";
                boolean deniedByPolicy = false;
                String deniedReason = "";
                AgentTool tool = toolRegistry.tools().stream()
                        .filter(t -> t.name().equals(safeName))
                        .findFirst()
                        .orElse(null);

                if (tool == null) {
                    status = "NOT_FOUND";
                    content = "Error: tool not found: " + safeName;
                }
                else if (toolPolicyService != null) {
                    ToolPolicyDecision decision = toolPolicyService.evaluateTool(safeName, activeSkills, view.mode());
                    if (decision.allowed()) {
                        ToolExecution execution = executeTool(tool, safeName, safeArguments, callId, uploadedFileIds, runId, threadId, view);
                        status = execution.status();
                        content = execution.content();
                        errorType = execution.errorType();
                    } else {
                        status = "DENIED";
                        deniedByPolicy = true;
                        deniedReason = decision.reason();
                        content = "Error: tool denied by policy: " + deniedReason;
                    }
                }
                else {
                    ToolExecution execution = executeTool(tool, safeName, safeArguments, callId, uploadedFileIds, runId, threadId, view);
                    status = execution.status();
                    content = execution.content();
                    errorType = execution.errorType();
                }

                long duration = System.currentTimeMillis() - startTime;
                Map<String, Object> metadata = resultMetadata(status, safeName, callId, duration, deniedByPolicy, deniedReason, errorType);
                ToolResult result = ToolResult.of(safeName, content, metadata);
                persistToolCallResult(callId, safeName, safeArguments, status, content, duration, metadata);

                GraphEventRegistry.publish(runId, AgentEvent.of(
                        UUID.randomUUID().toString(),
                        runId,
                        threadId,
                        deniedByPolicy ? AgentEventType.TOOL_DENIED : AgentEventType.TOOL_COMPLETED,
                        result.content(),
                        metadata
                ));

                Map<String, Object> toolMsg = new LinkedHashMap<>();
                toolMsg.put("messageId", UUID.randomUUID().toString());
                toolMsg.put("threadId", threadId);
                toolMsg.put("runId", runId);
                toolMsg.put("role", ModelMessage.Role.TOOL.name());
                toolMsg.put("content", result.content());
                toolMsg.put("name", safeName);
                toolMsg.put("toolCallId", callId);
                toolMsg.put("metadata", metadata);
                toolMsg.put("createdAt", java.time.Instant.now().toString());
                toolMessages.add(toolMsg);

                Map<String, Object> resMap = new LinkedHashMap<>();
                resMap.put("toolName", safeName);
                resMap.put("result", result.content());
                resMap.put("metadata", result.metadata());
                toolResultsList.add(resMap);
            }

            Map<String, Object> clarificationMeta = null;
            for (var res : toolResultsList) {
                Map<String, Object> meta = (Map<String, Object>) res.get("metadata");
                if (meta != null && Boolean.TRUE.equals(meta.get("clarificationRequired"))) {
                    clarificationMeta = meta;
                    break;
                }
            }

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.MESSAGE_WINDOW, toolMessages);
            update.put(AgentGraphStateKeys.TOOL_RESULTS, toolResultsList);
            update.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, List.of());
            if (clarificationMeta != null) {
                update.put("clarification_metadata", clarificationMeta);
            } else {
                update.put("clarification_metadata", Map.of());
            }
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "execute_tools", "status", "completed")));
            return update;
        }, executor);
    }

    private ToolExecution executeTool(AgentTool tool, String toolName, String arguments, String callId,
                                      List<String> uploadedFileIds, String runId, String threadId,
                                      AgentGraphStateView view) {
        GraphEventRegistry.publish(runId, AgentEvent.of(
                UUID.randomUUID().toString(),
                runId,
                threadId,
                AgentEventType.TOOL_STARTED,
                "Executing tool " + toolName,
                Map.of("toolCallId", callId, "toolName", toolName, "arguments", arguments)
        ));
        try {
            Path workspaceRoot = Path.of(properties.getWorkspaceRoot() != null ? properties.getWorkspaceRoot() : ".");
            ToolRequest toolRequest = new ToolRequest(
                    arguments,
                    workspaceRoot,
                    uploadedFileIds,
                    threadId,
                    runId,
                    view.mode(),
                    view.activeSkills(),
                    view.modelName()
            );
            ToolResult result = tool.execute(toolRequest);
            return new ToolExecution("SUCCESS", result.content(), "");
        }
        catch (Exception ex) {
            return new ToolExecution("FAILED", "Error executing tool " + toolName + ": " + ex.getMessage(),
                    ex.getClass().getSimpleName());
        }
    }

    private record ToolExecution(String status, String content, String errorType) {
    }

    private void persistToolCallRequested(String callId, String toolName, String arguments, String runId, String threadId) {
        if (toolCallStore == null) {
            return;
        }
        try {
            toolCallStore.saveRequested(new ToolCall(callId, toolName, arguments, ToolCall.Status.PENDING, Map.of()),
                    runId, threadId);
        }
        catch (Exception ignored) {
            // Best-effort audit only; graph execution must not fail because audit storage failed.
        }
    }

    private void persistToolCallResult(String callId, String toolName, String arguments, String status, String content,
                                       long durationMs, Map<String, Object> metadata) {
        if (toolCallStore == null) {
            return;
        }
        try {
            ToolCallResult.Status resultStatus = "SUCCESS".equals(status)
                    ? ToolCallResult.Status.SUCCESS
                    : ToolCallResult.Status.FAILED;
            String result = "SUCCESS".equals(status) ? content : "";
            String error = "SUCCESS".equals(status) ? "" : content;
            toolCallStore.saveResult(callId, new ToolCallResult(callId, toolName, arguments, resultStatus,
                    result, error, durationMs, metadata));
        }
        catch (Exception ignored) {
            // Best-effort audit only; graph execution must not fail because audit storage failed.
        }
    }

    private static Map<String, Object> resultMetadata(String status, String toolName, String callId, long durationMs,
                                                      boolean deniedByPolicy, String deniedReason, String errorType) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("status", status);
        metadata.put("toolName", toolName);
        metadata.put("toolCallId", callId);
        metadata.put("durationMs", durationMs);
        if (deniedByPolicy) {
            metadata.put("deniedByPolicy", true);
            metadata.put("denied", true);
            metadata.put("reason", deniedReason == null || deniedReason.isBlank() ? "Tool denied by policy" : deniedReason);
        }
        if (errorType != null && !errorType.isBlank()) {
            metadata.put("errorType", errorType);
        }
        return metadata;
    }
}
