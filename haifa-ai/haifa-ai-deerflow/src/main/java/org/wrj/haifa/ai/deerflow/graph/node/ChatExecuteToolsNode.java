package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
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
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionToolCall;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionToolResult;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.RunExecutionContext;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.RunExecutionContextRegistry;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ToolCallFilterResult;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.config.GraphExecutorProperties;
import org.wrj.haifa.ai.deerflow.completion.CompletionRequirement;
import org.wrj.haifa.ai.deerflow.completion.EvidenceRecord;
import org.wrj.haifa.ai.deerflow.completion.Freshness;
import org.wrj.haifa.ai.deerflow.completion.ToolCompletionContract;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ParallelSafeAgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyDecision;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import org.wrj.haifa.ai.deerflow.run.RunCancellationService;
import org.wrj.haifa.ai.deerflow.run.RunCancellationToken;
import org.wrj.haifa.ai.deerflow.tool.execution.ToolBatchExecutor;
import org.wrj.haifa.ai.deerflow.tool.execution.ToolBatchItemResult;
import org.wrj.haifa.ai.deerflow.tool.execution.ToolBatchRequest;
import org.wrj.haifa.ai.deerflow.tool.execution.ToolExecutionTask;
import org.wrj.haifa.ai.deerflow.tool.execution.ToolExecutionIdempotencyService;

@Component
public class ChatExecuteToolsNode implements AsyncNodeAction {

    private final ToolRegistry toolRegistry;
    private final DeerFlowProperties properties;
    private final ToolPolicyService toolPolicyService;

    @Autowired(required = false)
    private ToolCallStore toolCallStore;

    @Autowired(required = false)
    private RunCancellationService runCancellationService;

    @Autowired(required = false)
    private ToolBatchExecutor toolBatchExecutor;

    @Autowired(required = false)
    private GraphExecutorProperties executorProperties;

    @Autowired(required = false)
    private RunExecutionContextRegistry executionContextRegistry;

    @Autowired(required = false)
    private ToolExecutionIdempotencyService idempotencyService;

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
        String schedulingRunId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
        // This adapter may wait for the batch future; actual tools always run on the isolated tool executor.
        java.util.concurrent.Executor executor = graphExecutionManager != null
                ? graphExecutionManager.getModelExecutor(schedulingRunId) : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");


            throwIfCancelled(runId);

            AgentGraphStateView view = AgentGraphStateView.of(state);
            RunExecutionContext lifecycle = executionContextRegistry == null ? null
                    : executionContextRegistry.get(runId).orElse(null);
            List<String> uploadedFileIds = lifecycle != null
                    ? lifecycle.uploadedFileIds()
                    : view.list(AgentGraphStateKeys.UPLOADED_FILE_IDS).stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .toList();
            List<Map<String, Object>> pending = view.listOfMaps(AgentGraphStateKeys.PENDING_TOOL_CALLS);
            List<ToolCallFilterResult> filteredCalls = filterToolCalls(lifecycle, pending);
            List<Map<String, Object>> toolMessages = new ArrayList<>();
            List<Map<String, Object>> toolResultsList = new ArrayList<>();
            List<Map<String, Object>> completionRequirements = new ArrayList<>();
            List<Map<String, Object>> evidenceRecords = new ArrayList<>();
            Map<String, ToolBatchItemResult<ToolExecution>> batchResults = executeBatch(
                    filteredCalls, uploadedFileIds, runId, threadId, view, lifecycle);

            for (ToolCallFilterResult filtered : filteredCalls) {

                throwIfCancelled(runId);
                ExecutionToolCall toolCall = filtered.toolCall();
                String safeName = nullToEmpty(toolCall.toolName());
                String safeArguments = nullToEmpty(toolCall.arguments());
                List<ToolCompletionContract> completionContracts = completionContracts(safeName);
                if (!filtered.allowed()) {
                    String reason = nullToEmpty(filtered.reason()).isBlank() ? "Tool call rejected by middleware" : filtered.reason();
                    ExecutionToolResult deniedResult = failedResult(toolCall, reason, 0);
                    Map<String, Object> metadata = resultMetadata("REJECTED", safeName, toolCall.id(), 0, true, reason, "");
                    persistToolCallResult(deniedResult, metadata);
                    publishToolEvent(runId, threadId, AgentEventType.TOOL_DENIED, reason, metadata);
                    toolMessages.add(toolMessage(threadId, runId, safeName, toolCall.id(), reason, metadata));
                    toolResultsList.add(toolResultMap(safeName, reason, metadata));
                    appendCompletionRecords(runId, toolCall.id(), safeName, "REJECTED", reason, metadata,
                            completionContracts, completionRequirements, evidenceRecords);
                    continue;
                }

                ToolExecution execution = batchExecution(toolCall, batchResults.get(toolCall.id()));
                ExecutionToolResult rawResult = execution.rawResult();
                Map<String, Object> metadata = resultMetadata(execution.status(), safeName, toolCall.id(),
                        rawResult.durationMs(), execution.deniedByPolicy(), execution.deniedReason(), execution.errorType());
                metadata.putAll(rawResult.metadata());
                completionContracts = mergeCompletionContracts(completionContracts, runtimeCompletionContracts(metadata));
                persistToolCallResult(rawResult, metadata);

                String eventContent = rawResult.status() == ExecutionToolResult.Status.SUCCESS ? rawResult.result() : rawResult.error();
                List<AgentEvent> observerEvents = new ArrayList<>();
                String observation = notifyObserver(lifecycle, toolCall, rawResult, observerEvents, view.messageWindow(), eventContent);
                publishObserverEvents(runId, observerEvents);

                publishToolEvent(runId, threadId,
                        execution.deniedByPolicy() || rawResult.status() == ExecutionToolResult.Status.DENIED
                                ? AgentEventType.TOOL_DENIED
                                : rawResult.status() == ExecutionToolResult.Status.SUCCESS
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
                if ("SUCCESS".equalsIgnoreCase(execution.status())) {
                    appendDeclaredRequirements(metadata, completionRequirements);
                }
                appendCompletionRecords(runId, toolCall.id(), safeName, execution.status(), eventContent, metadata,
                        completionContracts, completionRequirements, evidenceRecords);
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
            update.put(AgentGraphStateKeys.COMPLETION_REQUIREMENTS, completionRequirements);
            update.put(AgentGraphStateKeys.EVIDENCE_LEDGER, evidenceRecords);
            update.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, List.of());
            update.put("clarification_metadata", clarificationMeta == null ? Map.of() : clarificationMeta);
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "execute_tools", "status", "completed")));
            return update;
        }, executor);
    }

    private Map<String, ToolBatchItemResult<ToolExecution>> executeBatch(
            List<ToolCallFilterResult> filteredCalls, List<String> uploadedFileIds,
            String runId, String threadId, AgentGraphStateView view, RunExecutionContext lifecycle) {
        List<ToolExecutionTask<ToolExecution>> tasks = new ArrayList<>();
        for (ToolCallFilterResult filtered : filteredCalls) {
            ExecutionToolCall call = filtered.toolCall();
            persistToolCallRequested(call.id(), nullToEmpty(call.toolName()), nullToEmpty(call.arguments()), runId, threadId);
            if (!filtered.allowed()) {
                continue;
            }
            AgentTool tool = findAgentTool(call.toolName());
            var mode = tool == null ? org.wrj.haifa.ai.deerflow.tool.execution.ToolConcurrencyMode.SERIAL_PER_RUN
                    : tool.concurrencyMode();
            String resourceKey = tool == null ? "" : tool.concurrencyResourceKey(call.arguments());
            tasks.add(new ToolExecutionTask<>(call.id(), mode, resourceKey,
                    () -> executeTool(call, uploadedFileIds, runId, threadId, view, lifecycle)));
        }

        RunCancellationToken token = runCancellationService == null
                ? new RunCancellationToken(runId) : runCancellationService.token(runId);
        List<ToolBatchItemResult<ToolExecution>> results;
        if (toolBatchExecutor == null) {
            results = tasks.stream().map(task -> {
                if (token.isCancelled()) {
                    return ToolBatchItemResult.<ToolExecution>cancelled(task.callId());
                }
                try {
                    return ToolBatchItemResult.success(task.callId(), task.action().get(), false);
                }
                catch (Throwable ex) {
                    return ToolBatchItemResult.<ToolExecution>failed(task.callId(), ex);
                }
            }).toList();
        }
        else {
            int maxConcurrency = executorProperties == null ? 1
                    : Math.max(1, executorProperties.getToolCorePoolSize());
            results = toolBatchExecutor.execute(new ToolBatchRequest<>(runId, tasks, maxConcurrency, token)).join().items();
        }
        Map<String, ToolBatchItemResult<ToolExecution>> indexed = new LinkedHashMap<>();
        results.forEach(result -> indexed.put(result.callId(), result));
        return indexed;
    }

    private ToolExecution batchExecution(ExecutionToolCall call, ToolBatchItemResult<ToolExecution> item) {
        if (item == null || item.status() == ToolBatchItemResult.Status.CANCELLED) {
            return failedExecution(call, "FAILED", "Tool execution cancelled", System.currentTimeMillis(), "Cancelled");
        }
        if (item.status() == ToolBatchItemResult.Status.FAILED) {
            Throwable error = item.error();
            return failedExecution(call, "FAILED", "Error executing tool " + call.toolName() + ": "
                    + (error == null ? "unknown error" : error.getMessage()), System.currentTimeMillis(),
                    error == null ? "ToolExecutionError" : error.getClass().getSimpleName());
        }
        ToolExecution execution = item.value();
        if (!item.lateCompletion()) {
            return execution;
        }
        Map<String, Object> metadata = new LinkedHashMap<>(execution.rawResult().metadata());
        metadata.put("lateCompletionAfterCancel", true);
        ExecutionToolResult raw = execution.rawResult();
        return new ToolExecution(execution.status(), new ExecutionToolResult(raw.id(), raw.toolName(), raw.arguments(),
                raw.status(), raw.result(), raw.error(), raw.durationMs(), metadata), execution.errorType(),
                execution.deniedByPolicy(), execution.deniedReason());
    }

    private AgentTool findAgentTool(String toolName) {
        return toolRegistry.tools().stream().filter(tool -> tool.name().equals(toolName)).findFirst().orElse(null);
    }

    private List<ToolCompletionContract> completionContracts(String toolName) {
        return toolRegistry.tools().stream()
                .filter(tool -> tool.name().equals(toolName))
                .findFirst()
                .map(AgentTool::completionContracts)
                .orElse(List.of());
    }

    private static void appendDeclaredRequirements(Map<String, Object> metadata,
            List<Map<String, Object>> completionRequirements) {
        if (metadata == null || !(metadata.get("declaredCompletionRequirements") instanceof List<?> values)) {
            return;
        }
        for (Object value : values) {
            if (value instanceof Map<?, ?> raw) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                raw.forEach((key, item) -> normalized.put(String.valueOf(key), item));
                CompletionRequirement.fromMap(normalized).ifPresent(requirement ->
                        completionRequirements.add(requirement.toMap()));
            }
        }
    }

    private static List<ToolCompletionContract> runtimeCompletionContracts(Map<String, Object> metadata) {
        if (metadata == null || !(metadata.get("runtimeCompletionContracts") instanceof List<?> values)) {
            return List.of();
        }
        List<ToolCompletionContract> result = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Map<?, ?> map) {
                ToolCompletionContract.fromMap(map).ifPresent(result::add);
            }
        }
        return List.copyOf(result);
    }

    private static List<ToolCompletionContract> mergeCompletionContracts(List<ToolCompletionContract> base,
            List<ToolCompletionContract> runtime) {
        Map<String, ToolCompletionContract> merged = new LinkedHashMap<>();
        java.util.stream.Stream.concat(
                        base == null ? java.util.stream.Stream.empty() : base.stream(),
                        runtime == null ? java.util.stream.Stream.empty() : runtime.stream())
                .forEach(contract -> merged.putIfAbsent(
                        contract.requirementType().name() + ':' + contract.successEvidenceType().name() + ':' + contract.subject(),
                        contract));
        return List.copyOf(merged.values());
    }

    private static void appendCompletionRecords(String runId, String toolCallId, String toolName, String status,
            String content, Map<String, Object> metadata, List<ToolCompletionContract> contracts,
            List<Map<String, Object>> requirements, List<Map<String, Object>> evidence) {
        if (contracts == null || contracts.isEmpty()) {
            return;
        }
        if (!"SUCCESS".equalsIgnoreCase(status)) {
            return;
        }
        for (ToolCompletionContract contract : contracts) {
            String requirementId = "req:tool:" + toolCallId + ':' + contract.requirementType().name();
            requirements.add(new CompletionRequirement(
                    requirementId,
                    contract.requirementType(),
                    contract.subject(),
                    Freshness.CURRENT_RUN,
                    Map.of("source", "tool-contract", "toolName", toolName, "sourceToolCallId", toolCallId))
                    .toMap());
            List<String> parentIds = stringList(metadata == null ? null : metadata.get("sourceEvidenceIds"));
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("toolName", toolName);
            attributes.put("status", status);
            copyIfPresent(metadata, attributes, "presentedArtifactIds");
            copyIfPresent(metadata, attributes, "artifactDeliverySucceeded");
            copyIfPresent(metadata, attributes, "exitCode");
            evidence.add(new EvidenceRecord(
                    "ev:tool:" + toolCallId + ':' + contract.successEvidenceType().name(),
                    contract.successEvidenceType(),
                    runId,
                    toolCallId,
                    "tool-result:" + toolCallId,
                    sha256(content),
                    Instant.now(),
                    parentIds,
                    attributes).toMap());
        }
    }

    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source != null && source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> source)) {
            return List.of();
        }
        return source.stream().map(ChatExecuteToolsNode::nullToEmpty).filter(item -> !item.isBlank()).toList();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(nullToEmpty(value).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }


    private void throwIfCancelled(String runId) {
        if (runCancellationService != null) {
            runCancellationService.throwIfCancelled(runId);
        }
    }

    private List<ToolCallFilterResult> filterToolCalls(RunExecutionContext lifecycle,
            List<Map<String, Object>> pending) {
        List<ExecutionToolCall> calls = pending == null ? List.of() : pending.stream()
                .map(call -> new ExecutionToolCall(
                        nullToEmpty(call.get("id")),
                        nullToEmpty(call.get("name")),
                        nullToEmpty(call.get("arguments")), Map.of()))
                .toList();
        if (lifecycle == null || lifecycle.hook() == null) {
            return calls.stream().map(call -> new ToolCallFilterResult(call, true, null)).toList();
        }
        return lifecycle.hook().filterToolCalls(lifecycle.runConfig(), calls);
    }

    private ToolExecution executeTool(ExecutionToolCall toolCall, List<String> uploadedFileIds, String runId, String threadId,
                                      AgentGraphStateView view, RunExecutionContext lifecycle) {
        String toolName = nullToEmpty(toolCall.toolName());
        publishToolEvent(runId, threadId, AgentEventType.TOOL_STARTED, "Executing tool " + toolName,
                Map.of("toolCallId", toolCall.id(), "toolName", toolName, "arguments", nullToEmpty(toolCall.arguments())));
        long startTime = System.currentTimeMillis();
        String idempotencyKey = "";
        boolean highRisk = true;
        try {
            if (lifecycle != null && lifecycle.hook() != null && lifecycle.runConfig() != null) {
                ExecutionToolResult observerBypass = lifecycle.hook().beforeTool(lifecycle.runConfig(), toolCall);
                if (observerBypass != null) {
                    long duration = observerBypass.durationMs() > 0 ? observerBypass.durationMs() : System.currentTimeMillis() - startTime;
                    ExecutionToolResult result = new ExecutionToolResult(toolCall.id(), toolName, toolCall.arguments(),
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
            if (lifecycle != null && !lifecycle.isToolAllowed(toolName)) {
                return deniedExecution(toolCall, "Tool is outside the child run allowlist", startTime);
            }
            ToolPolicyService effectivePolicy = lifecycle != null && lifecycle.toolPolicyService() != null
                    ? lifecycle.toolPolicyService() : toolPolicyService;
            if (effectivePolicy != null) {
                ToolPolicyDecision decision = effectivePolicy.evaluateTool(toolName, view.activeSkills(), view.mode());
                if (!decision.allowed()) {
                    String reason = decision.reason() == null || decision.reason().isBlank() ? "Tool denied by policy" : decision.reason();
                    ExecutionToolResult result = new ExecutionToolResult(toolCall.id(), toolName, toolCall.arguments(),
                            ExecutionToolResult.Status.DENIED, "", "Error: tool denied by policy: " + reason,
                            System.currentTimeMillis() - startTime, Map.of("denied", true, "reason", reason));
                    return new ToolExecution("DENIED", result, "", true, reason);
                }
            }

            highRisk = !(tool instanceof ParallelSafeAgentTool);
            if (idempotencyService != null) {
                var reservation = idempotencyService.reserve(runId, toolCall.id(), toolName,
                        toolCall.arguments(), highRisk);
                idempotencyKey = reservation.idempotencyKey();
                if (reservation.decision()
                        == ToolExecutionIdempotencyService.Decision.REPLAY_SUCCEEDED) {
                    ExecutionToolResult replay = new ExecutionToolResult(toolCall.id(), toolName, toolCall.arguments(),
                            ExecutionToolResult.Status.SUCCESS, reservation.result(), "",
                            System.currentTimeMillis() - startTime,
                            Map.of("idempotencyReplay", true, "idempotencyKey", idempotencyKey));
                    return new ToolExecution("SUCCESS", replay, "", false, "");
                }
                if (reservation.decision()
                        == ToolExecutionIdempotencyService.Decision.BLOCK_UNKNOWN_OUTCOME
                        || reservation.decision()
                        == ToolExecutionIdempotencyService.Decision.BLOCK_RETRY_POLICY) {
                    ExecutionToolResult blocked = new ExecutionToolResult(toolCall.id(), toolName, toolCall.arguments(),
                            ExecutionToolResult.Status.FAILED, "", reservation.reason(),
                            System.currentTimeMillis() - startTime,
                            Map.of("unknownOutcome", true, "automaticRetryBlocked", true,
                                    "idempotencyKey", idempotencyKey));
                    return new ToolExecution("UNKNOWN_OUTCOME", blocked, "UnknownOutcome", false, "");
                }
                idempotencyService.markRunning(idempotencyKey);
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
                    view.modelName(),
                    runCancellationService == null ? new RunCancellationToken(runId)
                            : runCancellationService.token(runId)
            );
            ToolResult result = tool.execute(toolRequest);
            long duration = System.currentTimeMillis() - startTime;
            ExecutionToolResult raw = ExecutionToolResult.from(toolCall, result, duration);
            if (idempotencyService != null && !idempotencyKey.isBlank()) {
                if (raw.status() == ExecutionToolResult.Status.SUCCESS) {
                    idempotencyService.markSucceeded(idempotencyKey, raw.result());
                } else {
                    idempotencyService.markFailed(idempotencyKey,
                            raw.error().isBlank() ? raw.result() : raw.error());
                }
            }
            return new ToolExecution(raw.status().name(), raw, "", raw.status() == ExecutionToolResult.Status.DENIED,
                    raw.status() == ExecutionToolResult.Status.DENIED ? result.content() : "");
        }
        catch (Exception ex) {
            if (idempotencyService != null && !idempotencyKey.isBlank()) {
                if (highRisk) {
                    idempotencyService.markUnknownOutcome(idempotencyKey, ex.getMessage());
                } else {
                    idempotencyService.markFailed(idempotencyKey, ex.getMessage());
                }
            }
            return failedExecution(toolCall, "FAILED", "Error executing tool " + toolName + ": " + ex.getMessage(), startTime,
                    ex.getClass().getSimpleName());
        }
    }

    private ToolExecution failedExecution(ExecutionToolCall toolCall, String status, String error, long startTime, String errorType) {
        ExecutionToolResult.Status rawStatus;
        try {
            rawStatus = ExecutionToolResult.Status.valueOf(status);
        } catch (IllegalArgumentException ex) {
            rawStatus = ExecutionToolResult.Status.FAILED;
        }
        ExecutionToolResult raw = new ExecutionToolResult(toolCall.id(), toolCall.toolName(), toolCall.arguments(),
                rawStatus, "", error, System.currentTimeMillis() - startTime,
                Map.of("failed", true, "errorType", errorType));
        return new ToolExecution(status, raw, errorType, false, "");
    }

    private ToolExecution deniedExecution(ExecutionToolCall toolCall, String reason, long startTime) {
        ExecutionToolResult result = new ExecutionToolResult(toolCall.id(), toolCall.toolName(), toolCall.arguments(),
                ExecutionToolResult.Status.DENIED, "", reason, System.currentTimeMillis() - startTime,
                Map.of("denied", true, "reason", reason));
        return new ToolExecution("DENIED", result, "", true, reason);
    }

    private static ExecutionToolResult failedResult(ExecutionToolCall call, String reason, long duration) {
        return new ExecutionToolResult(call.id(), call.toolName(), call.arguments(),
                ExecutionToolResult.Status.FAILED, "", reason, duration, Map.of("failed", true));
    }

    private String notifyObserver(RunExecutionContext lifecycle, ExecutionToolCall toolCall, ExecutionToolResult rawResult,
            List<AgentEvent> observerEvents, List<Map<String, Object>> window, String eventContent) {
        if (lifecycle == null || lifecycle.hook() == null || lifecycle.runConfig() == null) {
            return "";
        }
        List<String> history = historyFromWindow(window);
        history.add("Tool result (" + toolCall.toolName() + "): " + eventContent);
        return lifecycle.hook().afterTool(lifecycle.runConfig(), toolCall, rawResult,
                observerEvents, lifecycle.eventSequence(), history);
    }

    private void persistToolCallRequested(String callId, String toolName, String arguments, String runId, String threadId) {
        if (toolCallStore == null) {
            return;
        }
        try {
            toolCallStore.saveRequested(new ExecutionToolCall(callId, toolName, arguments, Map.of()),
                    runId, threadId);
        }
        catch (Exception ignored) {
            // Best-effort audit only; graph execution must not fail because audit storage failed.
        }
    }

    private void persistToolCallResult(ExecutionToolResult rawResult, Map<String, Object> metadata) {
        if (toolCallStore == null || rawResult == null) {
            return;
        }
        try {
            ExecutionToolResult persisted = new ExecutionToolResult(rawResult.id(), rawResult.toolName(), rawResult.arguments(),
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
            String toolCallId, ExecutionToolResult result, String content, Map<String, Object> metadata) {
        if (!"write_todos".equals(toolName) || result.status() != ExecutionToolResult.Status.SUCCESS
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

    private record ToolExecution(String status, ExecutionToolResult rawResult, String errorType,
                                 boolean deniedByPolicy, String deniedReason) {
    }
}
