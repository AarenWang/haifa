package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphChatLifecycleContext;
import org.wrj.haifa.ai.deerflow.graph.GraphChatLifecycleRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyDecision;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import org.wrj.haifa.ai.deerflow.run.RunCancellationService;

@Component
public class ChatExecuteToolsNode implements AsyncNodeAction {

    private final ToolRegistry toolRegistry;
    private final DeerFlowProperties properties;
    private final ToolPolicyService toolPolicyService;

    @Autowired(required = false)
    private ToolCallStore toolCallStore;

    @Autowired(required = false)
    private RunCancellationService runCancellationService;

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


            throwIfCancelled(runId);

            AgentGraphStateView view = AgentGraphStateView.of(state);
            GraphChatLifecycleContext lifecycle = GraphChatLifecycleRegistry.get(runId).orElse(null);
            List<String> uploadedFileIds = lifecycle != null
                    ? lifecycle.uploadedFileIds()
                    : view.list(AgentGraphStateKeys.UPLOADED_FILE_IDS).stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .toList();
            List<Map<String, Object>> pending = view.listOfMaps(AgentGraphStateKeys.PENDING_TOOL_CALLS);
            List<AgentLoopObserver.FilteredToolCall> filteredCalls = filterToolCalls(lifecycle, pending);
            List<Map<String, Object>> toolMessages = new ArrayList<>();
            List<Map<String, Object>> toolResultsList = new ArrayList<>();

            for (AgentLoopObserver.FilteredToolCall filtered : filteredCalls) {

                throwIfCancelled(runId);
                ToolCall toolCall = filtered.toolCall();
                String safeName = nullToEmpty(toolCall.toolName());
                String safeArguments = nullToEmpty(toolCall.arguments());
                persistToolCallRequested(toolCall.id(), safeName, safeArguments, runId, threadId);

                if (!filtered.allowed()) {
                    String reason = nullToEmpty(filtered.reason()).isBlank() ? "Tool call rejected by middleware" : filtered.reason();
                    ToolCallResult deniedResult = ToolCallResult.fromError(toolCall, reason, 0);
                    Map<String, Object> metadata = resultMetadata("REJECTED", safeName, toolCall.id(), 0, true, reason, "");
                    persistToolCallResult(deniedResult, metadata);
                    publishToolEvent(runId, threadId, AgentEventType.TOOL_DENIED, reason, metadata);
                    toolMessages.add(toolMessage(threadId, runId, safeName, toolCall.id(), reason, metadata));
                    toolResultsList.add(toolResultMap(safeName, reason, metadata));
                    continue;
                }

                ToolExecution execution = executeTool(toolCall, uploadedFileIds, runId, threadId, view, lifecycle);
                ToolCallResult rawResult = execution.rawResult();
                Map<String, Object> metadata = resultMetadata(execution.status(), safeName, toolCall.id(),
                        rawResult.durationMs(), execution.deniedByPolicy(), execution.deniedReason(), execution.errorType());
                metadata.putAll(rawResult.metadata());
                persistToolCallResult(rawResult, metadata);

                String eventContent = rawResult.status() == ToolCallResult.Status.SUCCESS ? rawResult.result() : rawResult.error();
                List<AgentEvent> observerEvents = new ArrayList<>();
                String observation = notifyObserver(lifecycle, toolCall, rawResult, observerEvents, view.messageWindow(), eventContent);
                publishObserverEvents(runId, observerEvents);

                publishToolEvent(runId, threadId,
                        execution.deniedByPolicy() || rawResult.status() == ToolCallResult.Status.DENIED
                                ? AgentEventType.TOOL_DENIED
                                : rawResult.status() == ToolCallResult.Status.SUCCESS
                                        ? AgentEventType.TOOL_COMPLETED : AgentEventType.TOOL_FAILED,
                        eventContent,
                        metadata);
                publishTodoMutationEventIfNeeded(runId, threadId, safeName, toolCall.id(), rawResult, eventContent,
                        metadata);

                toolMessages.add(toolMessage(threadId, runId, safeName, toolCall.id(), eventContent, metadata));
                if (observation != null && !observation.isBlank()) {
                    toolMessages.add(systemMessage(threadId, runId, observation,
                            Map.of("observer", "onToolCompleted", "tool", safeName)));
                }
                toolResultsList.add(toolResultMap(safeName, eventContent, metadata));
            }

            Map<String, Object> clarificationMeta = null;
            for (var res : toolResultsList) {
                Map<String, Object> meta = (Map<String, Object>) res.get("metadata");
                if (meta != null && Boolean.TRUE.equals(meta.get("clarificationRequired"))) {
                    clarificationMeta = meta;
                    break;
                }
            }

            throwIfCancelled(runId);
            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.MESSAGE_WINDOW, toolMessages);
            update.put(AgentGraphStateKeys.TOOL_RESULTS, toolResultsList);
            update.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, List.of());
            update.put("clarification_metadata", clarificationMeta == null ? Map.of() : clarificationMeta);
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "execute_tools", "status", "completed")));
            return update;
        }, executor);
    }


    private void throwIfCancelled(String runId) {
        if (runCancellationService != null) {
            runCancellationService.throwIfCancelled(runId);
        }
    }

    private List<AgentLoopObserver.FilteredToolCall> filterToolCalls(GraphChatLifecycleContext lifecycle,
            List<Map<String, Object>> pending) {
        List<ToolCall> calls = pending == null ? List.of() : pending.stream()
                .map(call -> ToolCall.of(
                        nullToEmpty(call.get("id")),
                        nullToEmpty(call.get("name")),
                        nullToEmpty(call.get("arguments"))))
                .toList();
        if (lifecycle == null || lifecycle.observer() == null) {
            return calls.stream().map(call -> new AgentLoopObserver.FilteredToolCall(call, true, null)).toList();
        }
        return lifecycle.observer().afterToolCallsParsed(lifecycle.runConfig(), calls);
    }

    private ToolExecution executeTool(ToolCall toolCall, List<String> uploadedFileIds, String runId, String threadId,
                                      AgentGraphStateView view, GraphChatLifecycleContext lifecycle) {
        String toolName = nullToEmpty(toolCall.toolName());
        publishToolEvent(runId, threadId, AgentEventType.TOOL_STARTED, "Executing tool " + toolName,
                Map.of("toolCallId", toolCall.id(), "toolName", toolName, "arguments", nullToEmpty(toolCall.arguments())));
        long startTime = System.currentTimeMillis();
        try {
            if (lifecycle != null && lifecycle.observer() != null && lifecycle.runConfig() != null) {
                ToolCallResult observerBypass = lifecycle.observer().beforeToolExecute(lifecycle.runConfig(), toolCall);
                if (observerBypass != null) {
                    long duration = observerBypass.durationMs() > 0 ? observerBypass.durationMs() : System.currentTimeMillis() - startTime;
                    ToolCallResult result = new ToolCallResult(toolCall.id(), toolName, toolCall.arguments(),
                            observerBypass.status(), observerBypass.result(), observerBypass.error(), duration, observerBypass.metadata());
                    return new ToolExecution(result.status().name(), result, "", false, "");
                }
            }

            AgentTool tool = toolRegistry.tools().stream()
                    .filter(t -> t.name().equals(toolName))
                    .findFirst()
                    .orElse(null);
            if (tool == null) {
                return failedExecution(toolCall, "NOT_FOUND", "Error: tool not found: " + toolName, startTime, "ToolNotFound");
            }
            if (toolPolicyService != null) {
                ToolPolicyDecision decision = toolPolicyService.evaluateTool(toolName, view.activeSkills(), view.mode());
                if (!decision.allowed()) {
                    String reason = decision.reason() == null || decision.reason().isBlank() ? "Tool denied by policy" : decision.reason();
                    ToolCallResult result = new ToolCallResult(toolCall.id(), toolName, toolCall.arguments(),
                            ToolCallResult.Status.DENIED, "", "Error: tool denied by policy: " + reason,
                            System.currentTimeMillis() - startTime, Map.of("denied", true, "reason", reason));
                    return new ToolExecution("DENIED", result, "", true, reason);
                }
            }

            Path workspaceRoot = Path.of(properties.getWorkspaceRoot() != null ? properties.getWorkspaceRoot() : ".");
            ToolRequest toolRequest = new ToolRequest(
                    toolCall.arguments(),
                    workspaceRoot,
                    uploadedFileIds,
                    threadId,
                    runId,
                    view.mode(),
                    lifecycle == null ? view.activeSkills() : lifecycle.activeSkills(),
                    view.modelName()
            );
            ToolResult result = tool.execute(toolRequest);
            long duration = System.currentTimeMillis() - startTime;
            ToolCallResult raw = ToolCallResult.from(toolCall, result, duration);
            return new ToolExecution(raw.status().name(), raw, "", raw.status() == ToolCallResult.Status.DENIED,
                    raw.status() == ToolCallResult.Status.DENIED ? result.content() : "");
        }
        catch (Exception ex) {
            return failedExecution(toolCall, "FAILED", "Error executing tool " + toolName + ": " + ex.getMessage(), startTime,
                    ex.getClass().getSimpleName());
        }
    }

    private ToolExecution failedExecution(ToolCall toolCall, String status, String error, long startTime, String errorType) {
        ToolCallResult.Status rawStatus;
        try {
            rawStatus = ToolCallResult.Status.valueOf(status);
        } catch (IllegalArgumentException ex) {
            rawStatus = ToolCallResult.Status.FAILED;
        }
        ToolCallResult raw = new ToolCallResult(toolCall.id(), toolCall.toolName(), toolCall.arguments(),
                rawStatus, "", error, System.currentTimeMillis() - startTime,
                Map.of("failed", true, "errorType", errorType));
        return new ToolExecution(status, raw, errorType, false, "");
    }

    private String notifyObserver(GraphChatLifecycleContext lifecycle, ToolCall toolCall, ToolCallResult rawResult,
            List<AgentEvent> observerEvents, List<Map<String, Object>> window, String eventContent) {
        if (lifecycle == null || lifecycle.observer() == null || lifecycle.runConfig() == null) {
            return "";
        }
        List<String> history = historyFromWindow(window);
        history.add("Tool result (" + toolCall.toolName() + "): " + eventContent);
        return lifecycle.observer().onToolCompleted(lifecycle.runConfig(), toolCall, rawResult,
                observerEvents, lifecycle.eventSequence(), history);
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

    private void persistToolCallResult(ToolCallResult rawResult, Map<String, Object> metadata) {
        if (toolCallStore == null || rawResult == null) {
            return;
        }
        try {
            ToolCallResult persisted = new ToolCallResult(rawResult.id(), rawResult.toolName(), rawResult.arguments(),
                    rawResult.status(), rawResult.result(), rawResult.error(), rawResult.durationMs(), metadata);
            toolCallStore.saveResult(rawResult.id(), persisted);
        }
        catch (Exception ignored) {
            // Best-effort audit only; graph execution must not fail because audit storage failed.
        }
    }

    private static void publishToolEvent(String runId, String threadId, AgentEventType type, String content,
            Map<String, Object> metadata) {
        GraphEventRegistry.publish(runId, AgentEvent.of(
                UUID.randomUUID().toString(), runId, threadId, type, content, metadata));
    }

    private static void publishTodoMutationEventIfNeeded(String runId, String threadId, String toolName,
            String toolCallId, ToolCallResult result, String content, Map<String, Object> metadata) {
        if (!"write_todos".equals(toolName) || result.status() != ToolCallResult.Status.SUCCESS
                || Boolean.TRUE.equals(result.metadata().get("error"))) {
            return;
        }
        String operation = String.valueOf(result.metadata().getOrDefault("todoOperation", "updated"));
        if ("read".equals(operation) || "ignored".equals(operation)) {
            return;
        }
        Map<String, Object> eventMetadata = new LinkedHashMap<>(metadata);
        eventMetadata.put("toolCallId", toolCallId);
        eventMetadata.put("toolName", toolName);
        AgentEventType eventType = "created".equals(operation)
                ? AgentEventType.TODO_CREATED
                : AgentEventType.TODO_UPDATED;
        publishToolEvent(runId, threadId, eventType, content, eventMetadata);
    }

    private static void publishObserverEvents(String runId, List<AgentEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (AgentEvent event : events) {
            GraphEventRegistry.publish(runId, event);
        }
        events.clear();
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

    private static Map<String, Object> toolMessage(String threadId, String runId, String toolName, String callId,
            String content, Map<String, Object> metadata) {
        Map<String, Object> toolMsg = new LinkedHashMap<>();
        toolMsg.put("messageId", UUID.randomUUID().toString());
        toolMsg.put("threadId", threadId);
        toolMsg.put("runId", runId);
        toolMsg.put("role", ModelMessage.Role.TOOL.name());
        toolMsg.put("content", content == null ? "" : content);
        toolMsg.put("name", toolName);
        toolMsg.put("toolCallId", callId);
        toolMsg.put("metadata", metadata == null ? Map.of() : metadata);
        toolMsg.put("createdAt", java.time.Instant.now().toString());
        return toolMsg;
    }

    private static Map<String, Object> systemMessage(String threadId, String runId, String content,
            Map<String, Object> metadata) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("messageId", UUID.randomUUID().toString());
        message.put("threadId", threadId);
        message.put("runId", runId);
        message.put("role", ModelMessage.Role.SYSTEM.name());
        message.put("content", content == null ? "" : content);
        message.put("metadata", metadata == null ? Map.of() : metadata);
        message.put("createdAt", java.time.Instant.now().toString());
        return message;
    }

    private static Map<String, Object> toolResultMap(String toolName, String content, Map<String, Object> metadata) {
        Map<String, Object> resMap = new LinkedHashMap<>();
        resMap.put("toolName", toolName);
        resMap.put("result", content == null ? "" : content);
        resMap.put("metadata", metadata == null ? Map.of() : metadata);
        return resMap;
    }

    private static List<String> historyFromWindow(List<Map<String, Object>> window) {
        List<String> history = new ArrayList<>();
        if (window == null) {
            return history;
        }
        for (Map<String, Object> message : window) {
            String role = nullToEmpty(message.get("role"));
            String content = nullToEmpty(message.get("content"));
            if (content.isBlank()) {
                continue;
            }
            history.add(roleLabel(role) + ": " + content);
        }
        return history;
    }

    private static String roleLabel(String role) {
        if ("ASSISTANT".equalsIgnoreCase(role)) return "Assistant";
        if ("TOOL".equalsIgnoreCase(role)) return "Tool result";
        if ("SYSTEM".equalsIgnoreCase(role)) return "System";
        return "User";
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record ToolExecution(String status, ToolCallResult rawResult, String errorType,
                                 boolean deniedByPolicy, String deniedReason) {
    }
}
