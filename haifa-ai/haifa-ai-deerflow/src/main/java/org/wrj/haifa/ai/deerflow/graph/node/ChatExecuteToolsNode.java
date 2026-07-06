package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
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

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
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
                AgentTool tool = toolRegistry.tools().stream()
                        .filter(t -> t.name().equals(safeName))
                        .findFirst()
                        .orElse(null);

                if (tool == null) {
                    status = "NOT_FOUND";
                    content = "Error: tool not found: " + safeName;
                }
                else if (toolPolicyService != null && !toolPolicyService.isToolAllowed(safeName, activeSkills, view.mode())) {
                    status = "DENIED";
                    deniedByPolicy = true;
                    content = "Error: tool denied by policy: " + safeName;
                }
                else {
                    GraphEventRegistry.publish(runId, AgentEvent.of(
                            UUID.randomUUID().toString(),
                            runId,
                            threadId,
                            AgentEventType.TOOL_STARTED,
                            "Executing tool " + safeName,
                            Map.of("toolCallId", callId, "toolName", safeName, "arguments", safeArguments)
                    ));
                    try {
                        Path workspaceRoot = Path.of(properties.getWorkspaceRoot() != null ? properties.getWorkspaceRoot() : ".");
                        ToolRequest toolRequest = new ToolRequest(
                                safeArguments,
                                workspaceRoot,
                                uploadedFileIds,
                                threadId,
                                runId,
                                view.mode(),
                                activeSkills
                        );
                        ToolResult result = tool.execute(toolRequest);
                        content = result.content();
                    } catch (Exception ex) {
                        status = "FAILED";
                        errorType = ex.getClass().getSimpleName();
                        content = "Error executing tool " + safeName + ": " + ex.getMessage();
                    }
                }

                long duration = System.currentTimeMillis() - startTime;
                Map<String, Object> metadata = resultMetadata(status, safeName, callId, duration, deniedByPolicy, errorType);
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

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.MESSAGE_WINDOW, toolMessages);
            update.put(AgentGraphStateKeys.TOOL_RESULTS, toolResultsList);
            update.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, List.of());
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "execute_tools", "status", "completed")));
            return update;
        });
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
                                                      boolean deniedByPolicy, String errorType) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("status", status);
        metadata.put("toolName", toolName);
        metadata.put("toolCallId", callId);
        metadata.put("durationMs", durationMs);
        if (deniedByPolicy) {
            metadata.put("deniedByPolicy", true);
            metadata.put("denied", true);
            metadata.put("reason", "Tool denied by policy");
        }
        if (errorType != null && !errorType.isBlank()) {
            metadata.put("errorType", errorType);
        }
        return metadata;
    }
}
