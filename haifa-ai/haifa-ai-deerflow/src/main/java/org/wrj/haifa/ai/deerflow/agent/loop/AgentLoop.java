package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.model.ModelToolDefinition;
import org.wrj.haifa.ai.deerflow.model.ModelProtocolState;
import org.wrj.haifa.ai.deerflow.model.ModelResponseAccumulator;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ModelStepStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.middleware.ToolOutputBudgetMiddleware;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyDecision;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStore;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStore;
import org.wrj.haifa.ai.deerflow.approval.ApprovalPolicyService;
import org.wrj.haifa.ai.deerflow.approval.ApprovalPolicyDecision;
import org.wrj.haifa.ai.deerflow.approval.ApprovalPolicyDecisionType;
import org.wrj.haifa.ai.deerflow.approval.ApprovalRequestRecord;
import org.wrj.haifa.ai.deerflow.approval.ApprovalCreateRequest;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStatus;
import org.wrj.haifa.ai.deerflow.approval.ApprovalDecisionType;
import org.wrj.haifa.ai.deerflow.approval.RiskLevel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

/**
 * Core agent loop that supports multi-turn model calls and tool execution.
 */
public class AgentLoop {

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AgentModelClient modelClient;
    private final ToolRegistry toolRegistry;
    private final ModelStepStore modelStepStore;
    private final ToolCallStore toolCallStore;
    private final AgentLoopRunStore agentLoopRunStore;
    private final AgentLoopObserver observer;
    private final ToolOutputBudgetMiddleware toolOutputBudgetMiddleware;
    private final ClarificationStore clarificationStore;
    private final ApprovalPolicyService approvalPolicyService;
    private final ApprovalStore approvalStore;
    private final PromptAssembler promptAssembler;

    public AgentLoop(AgentModelClient modelClient, ToolRegistry toolRegistry) {
        this(modelClient, toolRegistry, null, null, null, new NoopAgentLoopObserver(), null, null, null, null);
    }

    public AgentLoop(AgentModelClient modelClient, ToolRegistry toolRegistry,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore) {
        this(modelClient, toolRegistry, modelStepStore, toolCallStore, agentLoopRunStore, new NoopAgentLoopObserver(), null, null, null, null);
    }

    public AgentLoop(AgentModelClient modelClient, ToolRegistry toolRegistry,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            AgentLoopObserver observer) {
        this(modelClient, toolRegistry, modelStepStore, toolCallStore, agentLoopRunStore, observer, null, null, null, null);
    }

    public AgentLoop(AgentModelClient modelClient, ToolRegistry toolRegistry,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            AgentLoopObserver observer, ToolOutputBudgetMiddleware toolOutputBudgetMiddleware) {
        this(modelClient, toolRegistry, modelStepStore, toolCallStore, agentLoopRunStore, observer, toolOutputBudgetMiddleware, null, null, null);
    }

    public AgentLoop(AgentModelClient modelClient, ToolRegistry toolRegistry,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            AgentLoopObserver observer, ToolOutputBudgetMiddleware toolOutputBudgetMiddleware,
            ClarificationStore clarificationStore) {
        this(modelClient, toolRegistry, modelStepStore, toolCallStore, agentLoopRunStore, observer, toolOutputBudgetMiddleware, clarificationStore, null, null);
    }

    public AgentLoop(AgentModelClient modelClient, ToolRegistry toolRegistry,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore,
            AgentLoopObserver observer, ToolOutputBudgetMiddleware toolOutputBudgetMiddleware,
            ClarificationStore clarificationStore, ApprovalPolicyService approvalPolicyService,
            ApprovalStore approvalStore) {
        this.modelClient = modelClient;
        this.toolRegistry = toolRegistry;
        this.modelStepStore = modelStepStore;
        this.toolCallStore = toolCallStore;
        this.agentLoopRunStore = agentLoopRunStore;
        this.observer = observer != null ? observer : new NoopAgentLoopObserver();
        this.toolOutputBudgetMiddleware = toolOutputBudgetMiddleware;
        this.clarificationStore = clarificationStore;
        this.approvalPolicyService = approvalPolicyService;
        this.approvalStore = approvalStore;
        this.promptAssembler = new PromptAssembler();
    }

    public AgentLoopObserver observer() {
        return this.observer;
    }

    public Flux<AgentEvent> run(
            LoopConfig config,
            AgentRunConfig runConfig,
            String systemPrompt,
            String userMessage,
            AtomicInteger seq,
            ToolPolicyService toolPolicy,
            List<Skill> activeSkills,
            List<String> uploadedFileIds) {
        return Flux.<AgentEvent>create(sink -> {
            executeRun(config, runConfig, systemPrompt, userMessage, seq, toolPolicy,
                    activeSkills, uploadedFileIds, sink);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void executeRun(
            LoopConfig config,
            AgentRunConfig runConfig,
            String systemPrompt,
            String userMessage,
            AtomicInteger seq,
            ToolPolicyService toolPolicy,
            List<Skill> activeSkills,
            List<String> uploadedFileIds,
            FluxSink<AgentEvent> sink) {
        try {
            long loopStartTime = System.currentTimeMillis();
            List<String> history = new ArrayList<>();
            history.add("User: " + userMessage);
            List<ModelMessage> typedHistory = new ArrayList<>();
            typedHistory.add(new ModelMessage(ModelMessage.Role.USER, userMessage));

            if (agentLoopRunStore != null) {
                agentLoopRunStore.create(runConfig.runId(), runConfig.threadId(), config);
            }

            List<AgentEvent> events = new ArrayList<>();
            EventEmitter emitter = new EventEmitter(events, sink);
            int totalToolCalls = resumeFromApprovalIfPresent(
                    runConfig, seq, activeSkills, uploadedFileIds, history, typedHistory, emitter);

            ToolEnvironment toolEnvironment = buildToolEnvironment(
                    systemPrompt, toolPolicy, activeSkills, runConfig.mode());
            String fullSystemPrompt = toolEnvironment.systemPrompt();
            List<ModelToolDefinition> toolDefinitions = toolEnvironment.toolDefinitions();

            boolean stopped = false;
            String stopReason = null;
            String lastModelContent = "";

            for (int step = 0; step < config.maxSteps(); step++) {
                if (sink.isCancelled()) {
                    stopReason = "CANCELLED";
                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markCancelled(runConfig.runId(), "SINK_CANCELLED");
                    }
                    stopped = true;
                    break;
                }

                if (agentLoopRunStore != null) {
                    agentLoopRunStore.updateStepCount(runConfig.runId(), step + 1);
                }

                // Check timeout
                if (System.currentTimeMillis() - loopStartTime > config.timeoutMs()) {
                    stopReason = "TIMEOUT";
                    emitter.emit(event(seq, runConfig, AgentEventType.RUN_FAILED,
                            "Loop timeout exceeded after " + config.timeoutMs() + "ms",
                            Map.of("stopReason", stopReason, "steps", step)));
                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markTimeout(runConfig.runId());
                    }
                    stopped = true;
                    break;
                }

                // Check max tool calls
                if (totalToolCalls >= config.maxToolCalls()) {
                    stopReason = "MAX_TOOL_CALLS_REACHED";
                    emitter.emit(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
                            "Max tool calls reached",
                            Map.of("stopReason", stopReason, "steps", step, "totalToolCalls", totalToolCalls)));
                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markCompleted(runConfig.runId(), stopReason);
                    }
                    stopped = true;
                    break;
                }

                long modelStartTime = System.currentTimeMillis();
                emitter.emit(event(seq, runConfig, AgentEventType.MODEL_STARTED,
                        "Model step " + (step + 1) + "/" + config.maxSteps(),
                        Map.of("step", step + 1, "maxSteps", config.maxSteps())));

                PromptAssembler.Result assembly = this.promptAssembler.assemble(
                        fullSystemPrompt, runConfig.modelName(), typedHistory);
                ModelPrompt prompt = assembly.prompt().withToolDefinitions(toolDefinitions);
                String userPrompt = prompt.userPrompt();
                if (log.isDebugEnabled()) {
                    log.debug("Prompt assembled. runId={}, step={}, trace={}",
                            runConfig.runId(), step + 1, assembly.trace());
                }
                ModelResponse modelResponse;
                try {
                    modelResponse = invokeModel(prompt, step, seq, runConfig, emitter);
                } catch (Exception ex) {
                    log.error("Model call failed at step {}. runId={}", step, runConfig.runId(), ex);
                    stopReason = "ERROR";
                    emitter.emit(event(seq, runConfig, AgentEventType.RUN_FAILED,
                            "Model call failed: " + ex.getMessage(),
                            Map.of("stopReason", stopReason, "step", step, "errorType", ex.getClass().getName())));
                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markFailed(runConfig.runId(), stopReason);
                    }
                    stopped = true;
                    break;
                }
                long modelDuration = System.currentTimeMillis() - modelStartTime;

                if (modelResponse == null) {
                    modelResponse = new ModelResponse("");
                }

                String responseContent = modelResponse.content() == null ? "" : modelResponse.content();

                // Persist model step
                if (modelStepStore != null) {
                    try {
                        ModelStep modelStep = new ModelStep(step + 1, userPrompt, responseContent, List.of(), modelStartTime, modelDuration);
                        modelStepStore.save(modelStep, runConfig.runId(), runConfig.threadId());
                    } catch (Exception e) {
                        log.warn("Failed to persist model step: {}", e.getMessage());
                    }
                }

                // Extract structured tool calls. Assistant text is never parsed as an executable tool call.
                List<ToolCall> loopToolCalls = new ArrayList<>();
                if (modelResponse.toolCalls() != null && !modelResponse.toolCalls().isEmpty()) {
                    for (ModelToolCall mtc : modelResponse.toolCalls()) {
                        loopToolCalls.add(ToolCall.of(mtc.id(), mtc.name(), mtc.arguments()));
                    }
                }

                // --- Apply observer tool-call filtering (e.g. SubagentLimitMiddleware) ---
                List<AgentLoopObserver.FilteredToolCall> filteredToolCalls = observer.afterToolCallsParsed(runConfig, loopToolCalls);
                List<ToolCall> assistantToolCalls = filteredToolCalls.stream()
                        .map(AgentLoopObserver.FilteredToolCall::toolCall)
                        .toList();
                String assistantContent = responseContent;
                lastModelContent = assistantContent;
                history.add("Assistant: " + assistantContent);
                List<ModelToolCall> assistantModelToolCalls = toModelToolCalls(assistantToolCalls);
                java.util.Map<String, Object> msgMetadata = new java.util.LinkedHashMap<>();
                msgMetadata.put("step", step + 1);
                msgMetadata.put("modelDurationMs", modelDuration);
                typedHistory.add(new ModelMessage(
                        ModelMessage.Role.ASSISTANT,
                        assistantContent,
                        assistantModelToolCalls,
                        null,
                        null,
                        msgMetadata,
                        modelResponse.protocolState()));
                emitter.emit(event(seq, runConfig, AgentEventType.MODEL_DELTA,
                        assistantContent,
                        modelDeltaMetadata(step + 1, modelDuration, assistantModelToolCalls, modelResponse.protocolState()),
                        modelResponse.protocolState()));
                // Emit events for rejected tool calls
                for (AgentLoopObserver.FilteredToolCall ftc : filteredToolCalls) {
                    if (!ftc.allowed()) {
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_CALL_REQUESTED,
                                "Tool call rejected by policy: " + ftc.toolCall().toolName(),
                                Map.of("toolCallId", ftc.toolCall().id(), "toolName", ftc.toolCall().toolName(),
                                        "denied", true, "reason", ftc.reason() != null ? ftc.reason() : "rejected by middleware")));
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_DENIED,
                                ftc.reason() != null ? ftc.reason() : "Tool call rejected by middleware",
                                Map.of("toolCallId", ftc.toolCall().id(), "toolName", ftc.toolCall().toolName(),
                                        "status", "REJECTED", "denied", true,
                                        "reason", ftc.reason() != null ? ftc.reason() : "rejected by middleware")));
                        history.add("Tool result (" + ftc.toolCall().toolName() + "): " + (ftc.reason() != null ? ftc.reason() : "Rejected by middleware"));
                        typedHistory.add(toolMessage(ftc.toolCall().id(), ftc.toolCall().toolName(),
                                ftc.reason() != null ? ftc.reason() : "Rejected by middleware",
                                "REJECTED", Map.of("denied", true)));
                    }
                }
                // Keep only allowed calls for execution
                loopToolCalls = filteredToolCalls.stream()
                        .filter(AgentLoopObserver.FilteredToolCall::allowed)
                        .map(AgentLoopObserver.FilteredToolCall::toolCall)
                        .toList();

                // Process invalid tool calls
                if (modelResponse.invalidToolCalls() != null && !modelResponse.invalidToolCalls().isEmpty()) {
                    for (String invalidCall : modelResponse.invalidToolCalls()) {
                        String invalidCallId = java.util.UUID.randomUUID().toString();
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_CALL_REQUESTED,
                                "Invalid tool call: " + invalidCall,
                                Map.of("toolCallId", invalidCallId, "error", "Invalid tool call format or arguments")));
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                                "Error: Invalid tool call arguments",
                                Map.of("toolCallId", invalidCallId, "status", "FAILED", "error", "Invalid tool call arguments")));
                        history.add("Tool result (error): Invalid tool call arguments for " + invalidCall);
                        typedHistory.add(toolMessage(invalidCallId, "error",
                                "Invalid tool call arguments for " + invalidCall,
                                "FAILED", Map.of("error", "Invalid tool call arguments")));
                    }
                }

                if (loopToolCalls.isEmpty()) {
                    NoToolAction action = handleResponseWithoutTools(runConfig, seq, history, typedHistory,
                            events, emitter, modelResponse, step + 1, totalToolCalls, modelDuration);
                    if (action == NoToolAction.CONTINUE) {
                        continue;
                    }
                    stopped = true;
                    break;
                }

                ToolBatchOutcome toolBatch = executeToolBatch(
                        config, runConfig, seq, toolPolicy, activeSkills, uploadedFileIds,
                        history, typedHistory, events, emitter, loopToolCalls, totalToolCalls);
                totalToolCalls = toolBatch.totalToolCalls();
                if (toolBatch.stopped()) {
                    stopped = true;
                }

                if (stopped) {
                    break;
                }

                if (totalToolCalls >= config.maxToolCalls()) {
                    stopReason = "MAX_TOOL_CALLS_REACHED";
                    emitter.emit(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
                            "Max tool calls reached after " + totalToolCalls + " tool calls",
                            Map.of("stopReason", stopReason, "steps", step + 1, "totalToolCalls", totalToolCalls)));
                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markCompleted(runConfig.runId(), stopReason);
                    }
                    stopped = true;
                    break;
                }

                if (runConfig.mode() == RunMode.RESEARCH) {
                    emitter.emit(event(seq, runConfig, AgentEventType.RESEARCH_STEP_COMPLETED,
                            "Step " + (step + 1) + " completed",
                            Map.of("step", step + 1, "totalToolCalls", totalToolCalls)));
                }

                observer.onStepCompleted(runConfig, events, seq, step + 1);
                emitter.flush();
            }

            // If loop exhausted max steps without final answer
            if (!stopped) {
                stopReason = "MAX_STEPS_REACHED";
                observer.onMaxStepsReached(runConfig, lastModelContent, events, seq, config.maxSteps(), totalToolCalls);
                emitter.flush();
                emitter.emit(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
                        "Max steps reached without final answer",
                        Map.of("stopReason", stopReason, "maxSteps", config.maxSteps(), "totalToolCalls", totalToolCalls)));
                if (agentLoopRunStore != null) {
                    agentLoopRunStore.markCompleted(runConfig.runId(), stopReason);
                }
            }

            sink.complete();
        } catch (Exception ex) {
            sink.error(ex);
        }
    }

    private NoToolAction handleResponseWithoutTools(AgentRunConfig runConfig, AtomicInteger seq,
            List<String> history, List<ModelMessage> typedHistory, List<AgentEvent> events,
            EventEmitter emitter, ModelResponse modelResponse, int step, int totalToolCalls,
            long modelDuration) {
        String responseContent = modelResponse.content() == null ? "" : modelResponse.content();
        if (responseContent.isBlank()) {
            String instruction = "The model returned no tool call and no assistant content. Continue with either a structured tool call or a normal final answer.";
            history.add("System: " + instruction);
            typedHistory.add(new ModelMessage(ModelMessage.Role.SYSTEM, instruction,
                    Map.of("emptyModelResponse", true)));
            return NoToolAction.CONTINUE;
        }
        if (looksLikeProgressPlaceholder(responseContent)) {
            String instruction = "Do not stop on progress placeholder text. Continue with the needed structured tool call or provide the actual final answer.";
            history.add("System: " + instruction);
            typedHistory.add(new ModelMessage(ModelMessage.Role.SYSTEM, instruction,
                    Map.of("progressPlaceholderRejected", true)));
            return NoToolAction.CONTINUE;
        }

        int historySizeBeforeContinueCheck = history.size();
        boolean shouldContinue = observer.shouldContinue(
                runConfig, responseContent, events, seq, step, totalToolCalls, history);
        appendNewHistoryEntriesToTypedHistory(history, historySizeBeforeContinueCheck, typedHistory,
                Map.of("observer", "shouldContinue"));
        emitter.flush();
        if (shouldContinue) {
            return NoToolAction.CONTINUE;
        }

        String stopReason = runConfig.mode() == RunMode.CHAT ? "ASSISTANT_COMPLETED" : "NO_TOOL_CALLS";
        FinalAnswerDecision decision = observer.onFinalAnswerProposed(
                runConfig, responseContent, events, seq, step, totalToolCalls);
        emitter.flush();
        if (!decision.accepted()) {
            emitFinalAnswerRejected(seq, runConfig, emitter, history, decision, step, totalToolCalls);
            typedHistory.add(new ModelMessage(ModelMessage.Role.SYSTEM, retryInstruction(decision),
                    Map.of("todoGateBlocked", true)));
            return NoToolAction.CONTINUE;
        }

        FinalAnswerResult finalResult = observer.onFinalAnswerAccepted(
                runConfig, decision.answer(), events, seq, step, totalToolCalls);
        emitter.flush();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("step", step);
        metadata.put("modelDurationMs", modelDuration);
        metadata.put("stopReason", stopReason);
        if (finalResult.extraMetadata() != null) {
            metadata.putAll(finalResult.extraMetadata());
        }
        if (modelResponse.protocolState() != null && !modelResponse.protocolState().isEmpty()) {
            metadata.put("providerStatePresent", true);
        }

        emitter.emit(event(seq, runConfig, AgentEventType.MODEL_COMPLETED,
                finalResult.finalAnswer(), metadata, modelResponse.protocolState()));
        emitter.emit(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
                "Run completed",
                Map.of("stopReason", stopReason, "steps", step, "totalToolCalls", totalToolCalls)));
        if (agentLoopRunStore != null) {
            agentLoopRunStore.markCompleted(runConfig.runId(), stopReason);
        }
        return NoToolAction.STOP;
    }

    private int resumeFromApprovalIfPresent(AgentRunConfig runConfig, AtomicInteger seq,
            List<Skill> activeSkills, List<String> uploadedFileIds, List<String> history,
            List<ModelMessage> typedHistory, EventEmitter emitter) {
        if (runConfig.metadata() == null || !runConfig.metadata().containsKey("approvalId")
                || approvalStore == null || approvalPolicyService == null) {
            return 0;
        }

        String approvalId = (String) runConfig.metadata().get("approvalId");
        Optional<ApprovalRequestRecord> approval = approvalStore.find(approvalId);
        if (approval.isEmpty()) {
            return 0;
        }

        ApprovalRequestRecord record = approval.get();
        history.add("Assistant requested tool " + record.toolName() + " with id " + record.toolCallId());
        ModelProtocolState protocolState = ModelProtocolState.empty();
        if (record.metadata() != null && record.metadata().containsKey("protocolState")) {
            protocolState = ModelProtocolState.deserializeProtocolState(record.metadata().get("protocolState"));
        }

        java.util.Map<String, Object> assistantMetadata = new java.util.HashMap<>();
        assistantMetadata.put("resumedApproval", true);
        typedHistory.add(new ModelMessage(
                ModelMessage.Role.ASSISTANT,
                "",
                List.of(new ModelToolCall(record.toolCallId(), record.toolName(), record.argsJson())),
                null,
                null,
                assistantMetadata,
                protocolState));

        if (record.status() == ApprovalStatus.PENDING && record.expiresAt() != null
                && Instant.now().isAfter(record.expiresAt())) {
            record = approvalStore.markExpired(approvalId);
        }

        if (record.status() == ApprovalStatus.APPROVED) {
            return resumeApprovedTool(approvalId, record, runConfig, seq, activeSkills,
                    uploadedFileIds, history, typedHistory, emitter);
        }

        appendRejectedApproval(record, runConfig, seq, history, typedHistory, emitter);
        return 0;
    }

    private int resumeApprovedTool(String approvalId, ApprovalRequestRecord record,
            AgentRunConfig runConfig, AtomicInteger seq, List<Skill> activeSkills,
            List<String> uploadedFileIds, List<String> history, List<ModelMessage> typedHistory,
            EventEmitter emitter) {
        String calculatedHash = approvalPolicyService.hashArgs(record.toolName(), record.argsJson());
        if (!record.argsHash().equals(calculatedHash)) {
            approvalStore.markInvalidated(approvalId,
                    "Args hash mismatch: expected " + record.argsHash() + " but got " + calculatedHash);
            String error = "Error: args hash mismatch. Tool call parameter modification detected.";
            history.add("Tool result (" + record.toolName() + "): " + error);
            typedHistory.add(toolMessage(record.toolCallId(), record.toolName(), error,
                    "FAILED", Map.of("approvalInvalidated", true)));
            return 0;
        }

        approvalStore.markExecuted(approvalId);
        long toolStartTime = System.currentTimeMillis();
        ToolCall toolCall = ToolCall.of(record.toolCallId(), record.toolName(), record.argsJson());
        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                "Executing " + record.toolName() + " (Approved)",
                Map.of("toolCallId", record.toolCallId(), "toolName", record.toolName())));

        ToolCallResult rawToolResult;
        try {
            rawToolResult = executeTool(toolCall, runConfig, uploadedFileIds, activeSkills);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - toolStartTime;
            rawToolResult = ToolCallResult.fromError(toolCall, "Tool failed: " + ex.getMessage(), duration);
        }

        persistToolResult(toolCall, rawToolResult);
        String compressedResult = compressToolResult(toolCall, rawToolResult, runConfig, seq, emitter);
        Map<String, Object> completionMetadata = completionMetadata(toolCall, rawToolResult);
        String eventContent = toolEventContent(rawToolResult, compressedResult);
        emitter.emit(event(seq, runConfig,
                rawToolResult.status() == ToolCallResult.Status.SUCCESS
                        ? AgentEventType.TOOL_COMPLETED : AgentEventType.TOOL_FAILED,
                eventContent, completionMetadata));

        history.add("Tool result (" + record.toolName() + "): " + eventContent);
        typedHistory.add(toolMessage(toolCall.id(), record.toolName(), eventContent,
                rawToolResult.status().name(), rawToolResult.metadata()));
        emitter.flush();
        return 1;
    }

    private void appendRejectedApproval(ApprovalRequestRecord record, AgentRunConfig runConfig,
            AtomicInteger seq, List<String> history, List<ModelMessage> typedHistory, EventEmitter emitter) {
        String deniedMessage = "APPROVAL_DENIED: This action was not executed. Do not retry this action, rephrase it, switch tools, or bypass the user's decision.";
        if (record.status() == ApprovalStatus.EXPIRED) {
            deniedMessage = "APPROVAL_EXPIRED: This action was not executed. Do not retry this action, rephrase it, switch tools, or bypass the user's decision.";
            emitter.emit(event(seq, runConfig, AgentEventType.APPROVAL_EXPIRED,
                    "Approval expired for " + record.toolName(),
                    Map.of("approvalId", record.approvalId(), "toolCallId", record.toolCallId(),
                            "toolName", record.toolName(), "reason", "Approval timeout")));
        }

        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                "Policy denied " + record.toolName() + " (" + record.status() + ")",
                Map.of("toolCallId", record.toolCallId(), "toolName", record.toolName(), "denied", true)));
        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_DENIED,
                deniedMessage,
                Map.of("toolCallId", record.toolCallId(), "toolName", record.toolName(),
                        "status", "DENIED", "denied", true, "reason", record.status().name())));
        history.add("Tool result (" + record.toolName() + "): " + deniedMessage);
        typedHistory.add(toolMessage(record.toolCallId(), record.toolName(), deniedMessage,
                record.status().name(), Map.of("denied", true)));
        emitter.flush();
    }

    private ModelResponse invokeModel(ModelPrompt prompt, int step, AtomicInteger seq,
            AgentRunConfig runConfig, EventEmitter emitter) {
        ModelResponseAccumulator accumulator = new ModelResponseAccumulator();

        Flux<ModelResponse> stream = this.modelClient.streamGenerate(prompt);
        if (stream == null) {
            stream = this.modelClient.generate(prompt).flux();
        }
        stream.doOnNext(response -> {
            accumulator.accumulate(response);
            String chunk = response.content();
            if (chunk != null && !chunk.isEmpty()) {
                emitter.emit(event(seq, runConfig, AgentEventType.MODEL_DELTA,
                        chunk, Map.of("step", step + 1)));
            }
        }).then().block();

        return accumulator.toResponse();
    }

    private ToolEnvironment buildToolEnvironment(String systemPrompt, ToolPolicyService toolPolicy,
            List<Skill> activeSkills, RunMode runMode) {
        StringBuilder toolDescriptions = new StringBuilder();
        List<ModelToolDefinition> toolDefinitions = new ArrayList<>();
        for (AgentTool tool : toolRegistry.tools()) {
            String toolName = tool.name();
            if (toolPolicy != null && !toolPolicy.evaluateTool(toolName, activeSkills, runMode).allowed()) {
                continue;
            }
            toolDescriptions.append("- ").append(toolName).append(": ")
                    .append(tool.description()).append("\n");
            toolDefinitions.add(new ModelToolDefinition(toolName, tool.description(), tool.inputSchema()));
        }

        String promptReinforcement = "\nIf a user asks for information that can be measured from the local runtime or workspace, and a sandbox execution tool is available, do not claim you lack access. Use the smallest appropriate script, inspect the tool result, then answer from observed output. If the tool is disabled or denied, explain the configuration limitation.";
        String fullSystemPrompt = systemPrompt + "\n\nAvailable tools:\n" + toolDescriptions
                + "\nWhen you need to use a tool, use the model provider's structured tool-call channel. "
                + "Do not write tool calls manually as XML, JSON blocks, or prose.\n"
                + "When no further tool call is needed, respond with the final answer directly in normal assistant text."
                + promptReinforcement;
        return new ToolEnvironment(fullSystemPrompt, List.copyOf(toolDefinitions));
    }

    private void persistToolResult(ToolCall toolCall, ToolCallResult result) {
        if (toolCallStore == null) {
            return;
        }
        try {
            toolCallStore.saveResult(toolCall.id(), result);
        } catch (Exception e) {
            log.warn("Failed to persist tool call result: {}", e.getMessage());
        }
    }

    private String compressToolResult(ToolCall toolCall, ToolCallResult result,
            AgentRunConfig runConfig, AtomicInteger seq, EventEmitter emitter) {
        String compressedResult = result.result();
        if (toolOutputBudgetMiddleware == null || result.status() != ToolCallResult.Status.SUCCESS) {
            return compressedResult;
        }

        String processed = toolOutputBudgetMiddleware.processToolOutput(
                toolCall.toolName(), result.result(), runConfig.threadId(), runConfig.runId(),
                null, null, null);
        if (processed != null && processed.length() < result.result().length()) {
            compressedResult = processed;
            emitter.emit(event(seq, runConfig, AgentEventType.TOOL_OUTPUT_BUDGET_EXCEEDED,
                    "Tool output budget exceeded for: " + toolCall.toolName() + ". Compressed.",
                    Map.of("toolName", toolCall.toolName(), "compressed", true)));
        }
        return compressedResult;
    }

    private static String toolEventContent(ToolCallResult result, String compressedResult) {
        if (result.status() != ToolCallResult.Status.SUCCESS && result.error() != null) {
            return result.error();
        }
        return compressedResult;
    }

    private static Map<String, Object> completionMetadata(ToolCall toolCall, ToolCallResult result) {
        Map<String, Object> metadata = new HashMap<>(result.metadata());
        metadata.put("toolCallId", toolCall.id());
        metadata.put("toolName", toolCall.toolName());
        metadata.put("status", result.status().name());
        metadata.put("durationMs", result.durationMs());
        metadata.put("error", result.error() != null ? result.error() : "");
        return metadata;
    }

    private ToolCallResult executeTool(ToolCall toolCall, AgentRunConfig runConfig, List<String> uploadedFileIds,
            List<Skill> activeSkills) {
        ToolCallResult observerBypass = observer.beforeToolExecute(runConfig, toolCall);
        if (observerBypass != null) {
            return observerBypass;
        }

        String toolName = toolCall.toolName();
        AgentTool tool = findTool(toolName);
        if (tool == null) {
            log.warn("Tool not found: {}. Available: {}", toolName, toolRegistry.tools().stream().map(AgentTool::name).toList());
            return new ToolCallResult(toolCall.id(), toolName, toolCall.arguments(),
                    ToolCallResult.Status.NOT_FOUND, "", "Tool not found: " + toolName, 0,
                    Map.of("notFound", true));
        }

        ToolRequest request = new ToolRequest(toolCall.arguments(), runConfig.workspaceRoot(),
                uploadedFileIds == null ? List.of() : uploadedFileIds, runConfig.threadId(), runConfig.runId(),
                runConfig.mode(), activeSkills, runConfig.modelName());
        long startTime = System.currentTimeMillis();
        try {
            ToolResult result = tool.execute(request);
            long duration = System.currentTimeMillis() - startTime;
            return ToolCallResult.from(toolCall, result, duration);
        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Tool {} failed: {}", toolName, ex.getMessage());
            return ToolCallResult.fromError(toolCall, "Tool failed: " + ex.getMessage(), duration);
        }
    }

    private static String extractQuestion(String resultText) {
        if (resultText == null) {
            return null;
        }
        int idx = resultText.indexOf("Question:");
        if (idx >= 0) {
            String question = resultText.substring(idx + "Question:".length()).trim();
            // Stop at first newline if there are extra lines
            int nl = question.indexOf('\n');
            if (nl >= 0) {
                question = question.substring(0, nl).trim();
            }
            return question;
        }
        return null;
    }

    private AgentTool findTool(String toolName) {
        if (toolName == null) {
            return null;
        }
        String normalizedQuery = toolName.toLowerCase().replace("_", "").replace("-", "").trim();
        for (AgentTool tool : toolRegistry.tools()) {
            String normalizedToolName = tool.name().toLowerCase().replace("_", "").replace("-", "").trim();
            if (normalizedToolName.equals(normalizedQuery)) {
                return tool;
            }
        }
        if ("mock_search".equals(toolName) || "mocksearch".equals(normalizedQuery)) {
            return findTool("web_search");
        }
        if ("mock_fetch".equals(toolName) || "mockfetch".equals(normalizedQuery)) {
            return findTool("web_fetch");
        }
        return null;
    }

    private static List<ModelToolCall> toModelToolCalls(List<ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        return toolCalls.stream()
                .map(tc -> new ModelToolCall(tc.id(), tc.toolName(), tc.arguments()))
                .toList();
    }

    private static Map<String, Object> modelDeltaMetadata(int step, long modelDuration, List<ModelToolCall> toolCalls, ModelProtocolState protocolState) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("step", step);
        metadata.put("modelDurationMs", modelDuration);
        if (toolCalls != null && !toolCalls.isEmpty()) {
            metadata.put("tool_calls", serializeToolCalls(toolCalls));
            metadata.put("persistAssistantToolCalls", true);
        }
        if (protocolState != null && !protocolState.isEmpty()) {
            metadata.put("providerStatePresent", true);
        }
        return metadata;
    }

    private static List<Map<String, Object>> serializeToolCalls(List<ModelToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        return toolCalls.stream()
                .map(tc -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", tc.id() == null ? "" : tc.id());
                    item.put("name", tc.name() == null ? "" : tc.name());
                    item.put("arguments", tc.arguments() == null ? "{}" : tc.arguments());
                    item.put("type", tc.type() == null ? "tool_call" : tc.type());
                    return item;
                })
                .toList();
    }

    private static ModelMessage toolMessage(String toolCallId, String toolName, String content,
            String status, Map<String, Object> sourceMetadata) {
        Map<String, Object> metadata = new HashMap<>();
        if (sourceMetadata != null) {
            metadata.putAll(sourceMetadata);
        }
        metadata.put("tool_call_id", toolCallId == null ? "" : toolCallId);
        metadata.put("tool", toolName == null ? "unknown" : toolName);
        metadata.put("status", status == null ? "" : status);
        return new ModelMessage(ModelMessage.Role.TOOL, content, List.of(), toolCallId, toolName, metadata);
    }

    private static void appendNewHistoryEntriesToTypedHistory(List<String> history, int previousSize,
            List<ModelMessage> typedHistory, Map<String, Object> metadata) {
        if (history == null || typedHistory == null || previousSize >= history.size()) {
            return;
        }
        for (int i = Math.max(0, previousSize); i < history.size(); i++) {
            String entry = history.get(i);
            if (entry == null || entry.isBlank()) {
                continue;
            }
            typedHistory.add(toTypedHistoryEntry(entry, metadata));
        }
    }

    private static ModelMessage toTypedHistoryEntry(String entry, Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : metadata;
        if (entry.startsWith("System: ")) {
            return new ModelMessage(ModelMessage.Role.SYSTEM, entry.substring("System: ".length()), safeMetadata);
        }
        if (entry.startsWith("User: ")) {
            return new ModelMessage(ModelMessage.Role.USER, entry.substring("User: ".length()), safeMetadata);
        }
        if (entry.startsWith("Assistant: ")) {
            return new ModelMessage(ModelMessage.Role.ASSISTANT, entry.substring("Assistant: ".length()), safeMetadata);
        }
        if (entry.startsWith("Tool result ")) {
            return new ModelMessage(ModelMessage.Role.TOOL, entry, List.of(), null, null, safeMetadata);
        }
        return new ModelMessage(ModelMessage.Role.SYSTEM, entry, safeMetadata);
    }

    private static boolean looksLikeProgressPlaceholder(String content) {
        if (content == null) {
            return false;
        }
        String normalized = content.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.length() > 120) {
            return false;
        }
        return containsChars(normalized, 0x6b63, 0x5728, 0x542f, 0x52a8)
                || containsChars(normalized, 0x6267, 0x884c, 0x4e2d)
                || containsChars(normalized, 0x8bf7, 0x7a0d, 0x5019)
                || normalized.contains("please wait")
                || normalized.contains("working on it")
                || normalized.contains("starting the visualization")
                || normalized.contains("starting visualization")
                || normalized.matches(".*\\bin progress\\b.*");
    }

    private static boolean containsChars(String text, int... chars) {
        StringBuilder phrase = new StringBuilder();
        for (int ch : chars) {
            phrase.append((char) ch);
        }
        return text.contains(phrase);
    }

    private static String retryInstruction(FinalAnswerDecision decision) {
        return decision.retryInstruction() == null || decision.retryInstruction().isBlank()
                ? "Do not finish yet. Continue the pending todo work."
                : decision.retryInstruction();
    }

    private static AgentEvent event(AtomicInteger seq, AgentRunConfig config, AgentEventType type, String content,
            Map<String, Object> metadata) {
        return AgentEvent.of(Integer.toString(seq.incrementAndGet()), config.runId(), config.threadId(), type, content,
                metadata);
    }

    private static AgentEvent event(AtomicInteger seq, AgentRunConfig config, AgentEventType type, String content,
            Map<String, Object> metadata, ModelProtocolState protocolState) {
        return AgentEvent.internal(Integer.toString(seq.incrementAndGet()), config.runId(), config.threadId(), type,
                content, metadata, protocolState);
    }

    private static void emitFinalAnswerRejected(AtomicInteger seq, AgentRunConfig runConfig, EventEmitter emitter,
            List<String> history, FinalAnswerDecision decision, int step, int totalToolCalls) {
        String retryInstruction = retryInstruction(decision);
        history.add("System: " + retryInstruction);
        Map<String, Object> metadata = new HashMap<>(decision.metadata());
        metadata.put("step", step);
        metadata.put("totalToolCalls", totalToolCalls);
        metadata.put("stopReason", "TODO_GATE_BLOCKED");
        emitter.emit(event(seq, runConfig, AgentEventType.TODO_GATE_BLOCKED, retryInstruction, metadata));
    }

    private static void emitTodoMutationEventIfNeeded(AtomicInteger seq, AgentRunConfig runConfig, EventEmitter emitter,
            PendingToolExecution pending, ToolCallResult toolResult) {
        if (!"write_todos".equals(pending.targetToolName()) || toolResult.status() != ToolCallResult.Status.SUCCESS) {
            return;
        }
        if (Boolean.TRUE.equals(toolResult.metadata().get("error"))) {
            return;
        }
        String operation = String.valueOf(toolResult.metadata().getOrDefault("todoOperation", "updated"));
        if ("read".equals(operation) || "ignored".equals(operation)) {
            return;
        }
        AgentEventType type = "created".equals(operation) ? AgentEventType.TODO_CREATED : AgentEventType.TODO_UPDATED;
        Map<String, Object> metadata = new HashMap<>(toolResult.metadata());
        metadata.put("toolCallId", pending.toolCall().id());
        metadata.put("toolName", pending.targetToolName());
        emitter.emit(event(seq, runConfig, type, toolResult.result(), metadata));
    }

    private record PendingToolExecution(
            ToolCall toolCall,
            String targetToolName,
            long startedAtMs,
            CompletableFuture<ToolCallResult> resultFuture
    ) {}

    private record ToolEnvironment(
            String systemPrompt,
            List<ModelToolDefinition> toolDefinitions
    ) {}

    private record ToolBatchOutcome(
            int totalToolCalls,
            boolean stopped
    ) {}

    private enum NoToolAction {
        CONTINUE,
        STOP
    }

    private static class EventEmitter {
        private final List<AgentEvent> events;
        private final FluxSink<AgentEvent> sink;
        private int emittedCount;

        EventEmitter(List<AgentEvent> events, FluxSink<AgentEvent> sink) {
            this.events = events;
            this.sink = sink;
        }

        void emit(AgentEvent event) {
            this.events.add(event);
            this.sink.next(event);
            this.emittedCount = this.events.size();
        }

        void flush() {
            while (this.emittedCount < this.events.size()) {
                this.sink.next(this.events.get(this.emittedCount));
                this.emittedCount++;
            }
        }
    }

    private ToolBatchOutcome executeToolBatch(LoopConfig config, AgentRunConfig runConfig,
            AtomicInteger seq, ToolPolicyService toolPolicy, List<Skill> activeSkills,
            List<String> uploadedFileIds, List<String> history, List<ModelMessage> typedHistory,
            List<AgentEvent> events, EventEmitter emitter, List<ToolCall> loopToolCalls,
            int initialTotalToolCalls) {
        int totalToolCalls = initialTotalToolCalls;
        boolean stopped = false;

        // Execute tool calls. Calls from the same model response are independent, so
        // launch them together and merge observations back in the original order.
        List<PendingToolExecution> pendingExecutions = new ArrayList<>();
        for (ToolCall toolCall : loopToolCalls) {
            totalToolCalls++;
            if (totalToolCalls > config.maxToolCalls()) {
                break;
            }

            emitter.emit(event(seq, runConfig, AgentEventType.TOOL_CALL_REQUESTED,
                    "Tool call requested: " + toolCall.toolName(),
                    Map.of("toolCallId", toolCall.id(), "toolName", toolCall.toolName(), "arguments", toolCall.arguments())));

            // Persist tool call request
            if (toolCallStore != null) {
                try {
                    toolCallStore.saveRequested(toolCall, runConfig.runId(), runConfig.threadId());
                } catch (Exception e) {
                    log.warn("Failed to persist tool call request: {}", e.getMessage());
                }
            }

            // Resolve normalized target tool name
            AgentTool targetTool = findTool(toolCall.toolName());
            String targetToolName = targetTool != null ? targetTool.name() : toolCall.toolName();

            // Policy check
            ToolPolicyDecision toolDecision = toolPolicy == null
                    ? ToolPolicyDecision.allow()
                    : toolPolicy.evaluateTool(targetToolName, activeSkills, runConfig.mode());
            if (!toolDecision.allowed()) {
                emitter.emit(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                        "Policy denied " + targetToolName,
                        Map.of("toolCallId", toolCall.id(), "toolName", targetToolName, "denied", true)));
                String deniedMessage = "Tool denied by policy: " + toolDecision.reason();
                emitter.emit(event(seq, runConfig, AgentEventType.TOOL_DENIED,
                        deniedMessage,
                        Map.of("toolCallId", toolCall.id(), "toolName", targetToolName,
                                "status", "DENIED", "denied", true, "reason", toolDecision.reason())));
                history.add("Tool result (" + targetToolName + "): " + deniedMessage);
                typedHistory.add(toolMessage(toolCall.id(), targetToolName, deniedMessage,
                        "DENIED", Map.of("denied", true, "reason", toolDecision.reason())));
                continue;
            }

            // Approval Policy check
            if (approvalPolicyService != null && approvalStore != null) {
                ModelToolCall modelToolCall = new ModelToolCall(toolCall.id(), targetToolName, toolCall.arguments());
                ApprovalPolicyDecision approvalDecision = approvalPolicyService.evaluate(modelToolCall, targetTool, runConfig);

                if (approvalDecision.type() == ApprovalPolicyDecisionType.DENY) {
                    String denMsg = "POLICY_BLOCKED: " + approvalDecision.reason() + ". This action was not executed. Do not retry this action, rephrase it, switch tools, or bypass the user's decision.";
                    emitter.emit(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                            "Policy denied " + targetToolName + ": " + approvalDecision.reason(),
                            Map.of("toolCallId", toolCall.id(), "toolName", targetToolName, "denied", true)));
                    emitter.emit(event(seq, runConfig, AgentEventType.TOOL_DENIED,
                            denMsg,
                            Map.of("toolCallId", toolCall.id(), "toolName", targetToolName,
                                    "status", "DENIED", "denied", true,
                                    "reason", approvalDecision.reason() == null ? "" : approvalDecision.reason())));
                    history.add("Tool result (" + targetToolName + "): " + denMsg);
                    Map<String, Object> deniedMetadata = new HashMap<>();
                    deniedMetadata.put("denied", true);
                    deniedMetadata.put("reason", approvalDecision.reason() == null ? "" : approvalDecision.reason());
                    typedHistory.add(toolMessage(toolCall.id(), targetToolName, denMsg,
                            "DENIED", deniedMetadata));
                    continue;
                }

                if (approvalDecision.type() == ApprovalPolicyDecisionType.REQUIRE_APPROVAL) {
                    String calculatedHash = approvalPolicyService.hashArgs(targetToolName, toolCall.arguments());

                    // Find the last assistant message and its protocolState
                    ModelMessage lastAssistant = null;
                    for (int i = typedHistory.size() - 1; i >= 0; i--) {
                        if (typedHistory.get(i).role() == ModelMessage.Role.ASSISTANT) {
                            lastAssistant = typedHistory.get(i);
                            break;
                        }
                    }

                    Map<String, Object> reqMetadata = new HashMap<>();
                    if (approvalDecision.metadata() != null) {
                        reqMetadata.putAll(approvalDecision.metadata());
                    }
                    if (lastAssistant != null && lastAssistant.protocolState() != null && !lastAssistant.protocolState().isEmpty()) {
                        reqMetadata.put("protocolState", ModelProtocolState.serializeProtocolState(lastAssistant.protocolState()));
                    }

                    ApprovalCreateRequest createReq = new ApprovalCreateRequest(
                            runConfig.runId(),
                            runConfig.threadId(),
                            toolCall.id(),
                            targetToolName,
                            toolCall.arguments(),
                            calculatedHash,
                            approvalDecision.riskKey(),
                            approvalDecision.riskLevel(),
                            approvalDecision.reason(),
                            "",
                            approvalDecision.preview(),
                            reqMetadata
                    );

                    // Try to extract purpose from arguments
                    try {
                        JsonNode node = objectMapper.readTree(toolCall.arguments());
                        if (node.has("purpose")) {
                            createReq = new ApprovalCreateRequest(
                                    runConfig.runId(),
                                    runConfig.threadId(),
                                    toolCall.id(),
                                    targetToolName,
                                    toolCall.arguments(),
                                    calculatedHash,
                                    approvalDecision.riskKey(),
                                    approvalDecision.riskLevel(),
                                    approvalDecision.reason(),
                                    node.get("purpose").asText(),
                                    approvalDecision.preview(),
                                    reqMetadata
                            );
                        }
                    } catch (Exception e) {}

                    ApprovalRequestRecord record = approvalStore.create(createReq);

                    // Emit APPROVAL_REQUIRED event
                    emitter.emit(event(seq, runConfig, AgentEventType.APPROVAL_REQUIRED,
                            "Approval required: " + record.reason(),
                            Map.of(
                                    "approvalId", record.approvalId(),
                                    "toolCallId", record.toolCallId(),
                                    "toolName", record.toolName(),
                                    "riskLevel", record.riskLevel().name(),
                                    "reason", record.reason(),
                                    "purpose", record.purpose(),
                                    "preview", record.preview(),
                                    "argsHash", record.argsHash(),
                                    "expiresAt", record.expiresAt().toString()
                            )));

                    // Emit RUN_SUSPENDED event
                    emitter.emit(event(seq, runConfig, AgentEventType.RUN_SUSPENDED,
                            "Run suspended waiting for human approval.",
                            Map.of(
                                    "suspendReason", "APPROVAL_REQUIRED",
                                    "approvalId", record.approvalId(),
                                    "resumeThreadId", runConfig.threadId(),
                                    "resumeRunId", runConfig.runId()
                            )));

                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markSuspended(runConfig.runId(), "APPROVAL_REQUIRED");
                    }

                    stopped = true;
                    break;
                }
            }

            long toolStartTime = System.currentTimeMillis();
            emitter.emit(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                    "Executing " + targetToolName,
                    Map.of("toolCallId", toolCall.id(), "toolName", targetToolName)));

            if ("task".equals(targetToolName)) {
                emitter.emit(event(seq, runConfig, AgentEventType.SUBAGENT_STARTED,
                        "Subagent task started",
                        Map.of("toolCallId", toolCall.id(), "toolName", targetToolName,
                                "arguments", toolCall.arguments())));
            }

            CompletableFuture<ToolCallResult> future = CompletableFuture.supplyAsync(
                    () -> executeTool(toolCall, runConfig, uploadedFileIds, activeSkills));
            pendingExecutions.add(new PendingToolExecution(toolCall, targetToolName, toolStartTime, future));
        }

        for (PendingToolExecution pending : pendingExecutions) {
            ToolCall toolCall = pending.toolCall();
            ToolCallResult rawToolResult;
            try {
                rawToolResult = pending.resultFuture().join();
            } catch (RuntimeException ex) {
                long duration = System.currentTimeMillis() - pending.startedAtMs();
                rawToolResult = ToolCallResult.fromError(toolCall,
                        "Tool failed: " + ex.getMessage(), duration);
            }
            // --- Intercept ask_clarification and suspend the run ---
            if (rawToolResult.metadata() != null && Boolean.TRUE.equals(rawToolResult.metadata().get("clarificationRequired"))) {
                String question = (String) rawToolResult.metadata().get("question");
                String clarificationId = (String) rawToolResult.metadata().get("clarificationId");
                String type = (String) rawToolResult.metadata().getOrDefault("clarificationType", "missing_info");
                Object options = rawToolResult.metadata().getOrDefault("options", java.util.List.of());
                Object questions = rawToolResult.metadata().getOrDefault("questions", java.util.List.of());

                Map<String, Object> clarificationMetadata = new HashMap<>();
                clarificationMetadata.put("question", question);
                clarificationMetadata.put("clarificationType", type);
                clarificationMetadata.put("clarificationId", clarificationId);
                clarificationMetadata.put("resumeThreadId", runConfig.threadId());
                clarificationMetadata.put("resumeRunId", runConfig.runId());
                clarificationMetadata.put("options", options);
                clarificationMetadata.put("questions", questions);

                emitter.emit(event(seq, runConfig, AgentEventType.CLARIFICATION_REQUIRED,
                        "Clarification required: " + question,
                        clarificationMetadata));
                emitter.emit(event(seq, runConfig, AgentEventType.RUN_SUSPENDED,
                        "Run suspended waiting for user clarification.",
                        clarificationMetadata));
                if (agentLoopRunStore != null) {
                    agentLoopRunStore.markSuspended(runConfig.runId(), "CLARIFICATION_REQUIRED");
                }
                stopped = true;
                break;
            }


            // Persist raw tool call result for audit/debug
            persistToolResult(toolCall, rawToolResult);

            emitTodoMutationEventIfNeeded(seq, runConfig, emitter, pending, rawToolResult);

            // Add raw history entry for observer source/evidence processing
            history.add("Tool result (" + toolCall.toolName() + "): " + rawToolResult.result());

            // Observer processes source/evidence (uses raw toolResult, does NOT compress)
            int historySizeBeforeObserver = history.size();
            String observation = observer.onToolCompleted(runConfig, toolCall, rawToolResult, events, seq, history);
            emitter.flush();
            if (observation != null && !observation.isBlank()) {
                history.add(observation);
            }
            appendNewHistoryEntriesToTypedHistory(history, historySizeBeforeObserver, typedHistory,
                    Map.of("observer", "onToolCompleted", "tool", pending.targetToolName()));

            // Compress tool output BEFORE emitting TOOL_COMPLETED event and MessageStore persistence
            String compressedResult = compressToolResult(toolCall, rawToolResult, runConfig, seq, emitter);
            if (!compressedResult.equals(rawToolResult.result())) {
                int lastIdx = history.size() - 1;
                if (lastIdx >= 0 && history.get(lastIdx).startsWith("Tool result (" + toolCall.toolName() + "): ")) {
                    history.set(lastIdx, "Tool result (" + toolCall.toolName() + "): " + compressedResult);
                }
            }

            // Emit TOOL_COMPLETED event with compressed content (ensures MessageStore/toolExecutionStore also see compressed)
            String eventContent = toolEventContent(rawToolResult, compressedResult);
            Map<String, Object> completionMetadata = completionMetadata(toolCall, rawToolResult);

            if ("task".equals(pending.targetToolName())) {
                Object subagentStatus = completionMetadata.getOrDefault("subagentStatus", rawToolResult.status().name());
                emitter.emit(event(seq, runConfig, AgentEventType.SUBAGENT_COMPLETED,
                        "COMPLETED".equals(subagentStatus) || "SUCCESS".equals(subagentStatus)
                                ? "Subagent task completed"
                                : "Subagent task failed",
                        completionMetadata));
            }

            emitter.emit(event(seq, runConfig,
                    rawToolResult.status() == ToolCallResult.Status.SUCCESS
                            ? AgentEventType.TOOL_COMPLETED : AgentEventType.TOOL_FAILED,
                    eventContent,
                    completionMetadata));
            typedHistory.add(toolMessage(toolCall.id(), pending.targetToolName(), eventContent,
                    rawToolResult.status().name(), completionMetadata));
        }


        return new ToolBatchOutcome(totalToolCalls, stopped);
    }

}
