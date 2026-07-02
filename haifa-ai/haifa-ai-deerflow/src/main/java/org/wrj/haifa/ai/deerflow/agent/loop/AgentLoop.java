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
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ModelStepStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.middleware.ToolOutputBudgetMiddleware;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
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
    private final ToolCallParser toolCallParser;
    private final ModelStepStore modelStepStore;
    private final ToolCallStore toolCallStore;
    private final AgentLoopRunStore agentLoopRunStore;
    private final AgentLoopObserver observer;
    private final ToolOutputBudgetMiddleware toolOutputBudgetMiddleware;
    private final ClarificationStore clarificationStore;
    private final ApprovalPolicyService approvalPolicyService;
    private final ApprovalStore approvalStore;

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
        this.toolCallParser = new ToolCallParser();
        this.modelStepStore = modelStepStore;
        this.toolCallStore = toolCallStore;
        this.agentLoopRunStore = agentLoopRunStore;
        this.observer = observer != null ? observer : new NoopAgentLoopObserver();
        this.toolOutputBudgetMiddleware = toolOutputBudgetMiddleware;
        this.clarificationStore = clarificationStore;
        this.approvalPolicyService = approvalPolicyService;
        this.approvalStore = approvalStore;
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
            try {
            long loopStartTime = System.currentTimeMillis();
            List<String> history = new ArrayList<>();
            history.add("User: " + userMessage);

            if (agentLoopRunStore != null) {
                agentLoopRunStore.create(runConfig.runId(), runConfig.threadId(), config);
            }

            List<AgentEvent> events = new ArrayList<>();
            EventEmitter emitter = new EventEmitter(events, sink);
            int totalToolCalls = 0;

            // Check if resumed from approval
            if (runConfig.metadata() != null && runConfig.metadata().containsKey("approvalId") && approvalStore != null && approvalPolicyService != null) {
                String approvalId = (String) runConfig.metadata().get("approvalId");
                Optional<ApprovalRequestRecord> appOpt = approvalStore.find(approvalId);
                if (appOpt.isPresent()) {
                    ApprovalRequestRecord appRecord = appOpt.get();
                    
                    // Reconstruct assistant message that requested tool call
                    String assistantMsg = "Assistant: <tool_call name=\"" + appRecord.toolName() + "\">" + appRecord.argsJson() + "</tool_call>";
                    history.add(assistantMsg);
                    
                    // Auto-expire pending approvals that have passed their expiration time
                    if (appRecord.status() == ApprovalStatus.PENDING && appRecord.expiresAt() != null && Instant.now().isAfter(appRecord.expiresAt())) {
                        appRecord = approvalStore.markExpired(approvalId);
                    }
                    
                    // Verify the status
                    if (appRecord.status() == ApprovalStatus.APPROVED) {
                        // Re-verify hash!
                        String calculatedHash = approvalPolicyService.hashArgs(appRecord.toolName(), appRecord.argsJson());
                        if (appRecord.argsHash().equals(calculatedHash)) {
                            // Mark as EXECUTED
                            approvalStore.markExecuted(approvalId);

                            // Execute the tool call
                            long toolStartTime = System.currentTimeMillis();
                            ToolCall toolCall = ToolCall.of(appRecord.toolCallId(), appRecord.toolName(), appRecord.argsJson());
                            
                            // Emit start event
                            emitter.emit(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                                    "Executing " + appRecord.toolName() + " (Approved)",
                                    Map.of("toolCallId", appRecord.toolCallId(), "toolName", appRecord.toolName())));
                            
                            ToolCallResult rawToolResult;
                            try {
                                rawToolResult = executeTool(toolCall, runConfig, uploadedFileIds, activeSkills);
                            } catch (Exception ex) {
                                long duration = System.currentTimeMillis() - toolStartTime;
                                rawToolResult = ToolCallResult.fromError(toolCall, "Tool failed: " + ex.getMessage(), duration);
                            }
                            
                            long toolDuration = rawToolResult.durationMs();
                            
                            // Persist result
                            if (toolCallStore != null) {
                                try {
                                    toolCallStore.saveResult(toolCall.id(), rawToolResult);
                                } catch (Exception e) {
                                    log.warn("Failed to persist tool call result: {}", e.getMessage());
                                }
                            }
                            
                            // Compress output
                            String compressedResult = rawToolResult.result();
                            if (toolOutputBudgetMiddleware != null && rawToolResult.status() == ToolCallResult.Status.SUCCESS) {
                                String processed = toolOutputBudgetMiddleware.processToolOutput(
                                        toolCall.toolName(), rawToolResult.result(), runConfig.threadId(), runConfig.runId(), null, null, null);
                                if (processed != null && processed.length() < rawToolResult.result().length()) {
                                    compressedResult = processed;
                                    emitter.emit(event(seq, runConfig, AgentEventType.TOOL_OUTPUT_BUDGET_EXCEEDED,
                                            "Tool output budget exceeded for: " + toolCall.toolName() + ". Compressed.",
                                            Map.of("toolName", toolCall.toolName(), "compressed", true)));
                                }
                            }
                            
                            String eventContent = compressedResult;
                            if (rawToolResult.status() == ToolCallResult.Status.FAILED && rawToolResult.error() != null) {
                                eventContent = rawToolResult.error();
                            }
                            
                            Map<String, Object> completionMetadata = new HashMap<>(rawToolResult.metadata());
                            completionMetadata.put("toolCallId", toolCall.id());
                            completionMetadata.put("toolName", toolCall.toolName());
                            completionMetadata.put("status", rawToolResult.status().name());
                            completionMetadata.put("durationMs", toolDuration);
                            completionMetadata.put("error", rawToolResult.error() != null ? rawToolResult.error() : "");
                            
                            emitter.emit(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                                    eventContent,
                                    completionMetadata));
                            
                            // Append tool result to history
                            history.add("Tool result (" + appRecord.toolName() + "): " + compressedResult);
                            totalToolCalls++;
                            
                            emitter.flush();
                        } else {
                            // INVALIDATED due to args hash mismatch!
                            approvalStore.markInvalidated(approvalId, "Args hash mismatch: expected " + appRecord.argsHash() + " but got " + calculatedHash);
                            history.add("Tool result (" + appRecord.toolName() + "): Error: args hash mismatch. Tool call parameter modification detected.");
                        }
                    } else {
                        // DENIED, EXPIRED, CANCELLED etc.
                        String denMsg = "APPROVAL_DENIED: This action was not executed. Do not retry this action, rephrase it, switch tools, or bypass the user's decision.";
                        if (appRecord.status() == ApprovalStatus.EXPIRED) {
                            denMsg = "APPROVAL_EXPIRED: This action was not executed. Do not retry this action, rephrase it, switch tools, or bypass the user's decision.";
                            emitter.emit(event(seq, runConfig, AgentEventType.APPROVAL_EXPIRED,
                                    "Approval expired for " + appRecord.toolName(),
                                    Map.of(
                                            "approvalId", appRecord.approvalId(),
                                            "toolCallId", appRecord.toolCallId(),
                                            "toolName", appRecord.toolName(),
                                            "reason", "Approval timeout"
                                    )));
                        }
                        
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                                "Policy denied " + appRecord.toolName() + " (" + appRecord.status() + ")",
                                Map.of("toolCallId", appRecord.toolCallId(), "toolName", appRecord.toolName(), "denied", true)));
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                                denMsg,
                                Map.of("toolCallId", appRecord.toolCallId(), "toolName", appRecord.toolName(), "denied", true)));
                        
                        history.add("Tool result (" + appRecord.toolName() + "): " + denMsg);
                        emitter.flush();
                    }
                }
            }

            StringBuilder toolDescriptions = new StringBuilder();
            for (AgentTool tool : toolRegistry.tools()) {
                String toolName = tool.name();
                if (toolPolicy != null && !toolPolicy.isToolAllowed(toolName, activeSkills, runConfig.mode())) {
                    continue;
                }
                toolDescriptions.append("- ").append(toolName).append(": ").append(tool.description()).append("\n");
            }

            String promptReinforcement = "\nIf a user asks for information that can be measured from the local runtime or workspace, and a sandbox execution tool is available, do not claim you lack access. Use the smallest appropriate script, inspect the tool result, then answer from observed output. If the tool is disabled or denied, explain the configuration limitation.";
            String fullSystemPrompt = systemPrompt + "\n\nAvailable tools:\n" + toolDescriptions
                    + "\nWhen you need to use a tool, emit: <tool_call name=\"tool_name\">{\"arg\":\"value\"}</tool_call>\n"
                    + "Do not write tool calls as prose such as `Tool call: name({...})`; use only the XML tag format above.\n"
                    + "When you have enough information, provide your final answer starting with <final_answer>."
                    + promptReinforcement;

            boolean stopped = false;
            String stopReason = null;
            String lastModelContent = "";

            for (int step = 0; step < config.maxSteps(); step++) {
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

                // Build prompt from history
                StringBuilder userPromptBuilder = new StringBuilder();
                for (String msg : history) {
                    userPromptBuilder.append(msg).append("\n\n");
                }
                String userPrompt = userPromptBuilder.toString().trim();

                long modelStartTime = System.currentTimeMillis();
                emitter.emit(event(seq, runConfig, AgentEventType.MODEL_STARTED,
                        "Model step " + (step + 1) + "/" + config.maxSteps(),
                        Map.of("step", step + 1, "maxSteps", config.maxSteps())));

                ModelPrompt prompt = new ModelPrompt(fullSystemPrompt, userPrompt, runConfig.modelName());
                ModelResponse modelResponse;
                try {
                    modelResponse = modelClient.generate(prompt).block();
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

                lastModelContent = modelResponse.content();

                // Persist model step
                if (modelStepStore != null) {
                    try {
                        ModelStep modelStep = new ModelStep(step + 1, userPrompt, modelResponse.content(), List.of(), modelStartTime, modelDuration);
                        modelStepStore.save(modelStep, runConfig.runId(), runConfig.threadId());
                    } catch (Exception e) {
                        log.warn("Failed to persist model step: {}", e.getMessage());
                    }
                }

                history.add("Assistant: " + modelResponse.content());
                emitter.emit(event(seq, runConfig, AgentEventType.MODEL_DELTA,
                        modelResponse.content(),
                        Map.of("step", step + 1, "modelDurationMs", modelDuration)));

                // Check for final answer
                if (toolCallParser.hasFinalAnswer(modelResponse.content())) {
                    String rawAnswer = toolCallParser.extractFinalAnswer(modelResponse.content());
                    FinalAnswerDecision decision = observer.onFinalAnswerProposed(
                            runConfig, rawAnswer, events, seq, step + 1, totalToolCalls);
                    emitter.flush();
                    if (!decision.accepted()) {
                        emitFinalAnswerRejected(seq, runConfig, emitter, history, decision, step + 1, totalToolCalls);
                        continue;
                    }
                    FinalAnswerResult finalResult = observer.onFinalAnswerAccepted(
                            runConfig, decision.answer(), events, seq, step + 1, totalToolCalls);
                    emitter.flush();
                    stopReason = "FINAL_ANSWER";

                    java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                    metadata.put("step", step + 1);
                    metadata.put("modelDurationMs", modelDuration);
                    metadata.put("stopReason", stopReason);
                    if (finalResult.extraMetadata() != null) {
                        metadata.putAll(finalResult.extraMetadata());
                    }

                    emitter.emit(event(seq, runConfig, AgentEventType.MODEL_COMPLETED,
                            finalResult.finalAnswer(),
                            metadata));
                    emitter.emit(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
                            "Run completed",
                            Map.of("stopReason", stopReason, "steps", step + 1, "totalToolCalls", totalToolCalls)));
                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markCompleted(runConfig.runId(), stopReason);
                    }
                    stopped = true;
                    break;
                }

                // Extract tool calls
                List<ToolCall> loopToolCalls = new ArrayList<>();
                if (modelResponse.toolCalls() != null && !modelResponse.toolCalls().isEmpty()) {
                    for (ModelToolCall mtc : modelResponse.toolCalls()) {
                        loopToolCalls.add(ToolCall.of(mtc.id(), mtc.name(), mtc.arguments()));
                    }
                } else if (modelResponse.content() != null && !modelResponse.content().isBlank()) {
                    List<ToolCallParser.ParsedToolCall> parsed = toolCallParser.parse(modelResponse.content());
                    for (ToolCallParser.ParsedToolCall ptc : parsed) {
                        loopToolCalls.add(ToolCall.of(ptc.toolName(), ptc.arguments()));
                    }
                }

                // --- Apply observer tool-call filtering (e.g. SubagentLimitMiddleware) ---
                List<AgentLoopObserver.FilteredToolCall> filteredToolCalls = observer.afterToolCallsParsed(runConfig, loopToolCalls);
                // Emit events for rejected tool calls
                for (AgentLoopObserver.FilteredToolCall ftc : filteredToolCalls) {
                    if (!ftc.allowed()) {
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_CALL_REQUESTED,
                                "Tool call rejected by policy: " + ftc.toolCall().toolName(),
                                Map.of("toolCallId", ftc.toolCall().id(), "toolName", ftc.toolCall().toolName(),
                                        "denied", true, "reason", ftc.reason() != null ? ftc.reason() : "rejected by middleware")));
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                                ftc.reason() != null ? ftc.reason() : "Tool call rejected by middleware",
                                Map.of("toolCallId", ftc.toolCall().id(), "toolName", ftc.toolCall().toolName(),
                                        "status", "REJECTED", "denied", true)));
                        history.add("Tool result (" + ftc.toolCall().toolName() + "): " + (ftc.reason() != null ? ftc.reason() : "Rejected by middleware"));
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
                    }
                }

                if (loopToolCalls.isEmpty()) {
                    if (toolCallParser.hasToolCallIntent(modelResponse.content())) {
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_CALL_REQUESTED,
                                "Unparsed tool call format detected. Asking model to re-emit tool calls.",
                                Map.of("step", step + 1, "unparsedToolCall", true)));
                        history.add("System: Your previous response looked like a tool call but could not be parsed. "
                                + "Re-emit each tool call exactly as "
                                + "<tool_call name=\"tool_name\">{\"arg\":\"value\"}</tool_call>. "
                                + "Do not describe the tool call in prose and do not answer yet.");
                        continue;
                    }
                    boolean continueAfterNoTools = observer.shouldContinue(
                            runConfig, modelResponse.content(), events, seq, step + 1, totalToolCalls, history);
                    emitter.flush();
                    if (continueAfterNoTools) {
                        continue;
                    }
                    stopReason = runConfig.mode() == RunMode.CHAT ? "ASSISTANT_COMPLETED" : "NO_TOOL_CALLS";
                    FinalAnswerDecision decision = observer.onFinalAnswerProposed(
                            runConfig, modelResponse.content(), events, seq, step + 1, totalToolCalls);
                    emitter.flush();
                    if (!decision.accepted()) {
                        emitFinalAnswerRejected(seq, runConfig, emitter, history, decision, step + 1, totalToolCalls);
                        continue;
                    }
                    FinalAnswerResult finalResult = observer.onFinalAnswerAccepted(
                            runConfig, decision.answer(), events, seq, step + 1, totalToolCalls);
                    emitter.flush();

                    java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                    metadata.put("step", step + 1);
                    metadata.put("modelDurationMs", modelDuration);
                    metadata.put("stopReason", stopReason);
                    if (finalResult.extraMetadata() != null) {
                        metadata.putAll(finalResult.extraMetadata());
                    }

                    emitter.emit(event(seq, runConfig, AgentEventType.MODEL_COMPLETED,
                            finalResult.finalAnswer(),
                            metadata));
                    emitter.emit(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
                            "Run completed",
                            Map.of("stopReason", stopReason, "steps", step + 1, "totalToolCalls", totalToolCalls)));
                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markCompleted(runConfig.runId(), stopReason);
                    }
                    stopped = true;
                    break;
                }

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
                    if (toolPolicy != null && !toolPolicy.isToolAllowed(targetToolName, activeSkills, runConfig.mode())) {
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                                "Policy denied " + targetToolName,
                                Map.of("toolCallId", toolCall.id(), "toolName", targetToolName, "denied", true)));
                        emitter.emit(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                                "Tool denied by policy",
                                Map.of("toolCallId", toolCall.id(), "toolName", targetToolName, "denied", true)));
                        history.add("Tool result (" + targetToolName + "): Tool denied by policy");
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
                            emitter.emit(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                                    denMsg,
                                    Map.of("toolCallId", toolCall.id(), "toolName", targetToolName, "denied", true)));
                            history.add("Tool result (" + targetToolName + "): " + denMsg);
                            continue;
                        }
                        
                        if (approvalDecision.type() == ApprovalPolicyDecisionType.REQUIRE_APPROVAL) {
                            String calculatedHash = approvalPolicyService.hashArgs(targetToolName, toolCall.arguments());
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
                                    approvalDecision.metadata()
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
                                            approvalDecision.metadata()
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
                    long toolDuration = rawToolResult.durationMs();

                    // --- Intercept ask_clarification and suspend the run ---
                    if (rawToolResult.metadata() != null && Boolean.TRUE.equals(rawToolResult.metadata().get("clarificationRequired"))) {
                        String question = (String) rawToolResult.metadata().get("question");
                        String clarificationId = (String) rawToolResult.metadata().get("clarificationId");
                        String type = (String) rawToolResult.metadata().getOrDefault("clarificationType", "missing_info");

                        emitter.emit(event(seq, runConfig, AgentEventType.CLARIFICATION_REQUIRED,
                                "Clarification required: " + question,
                                Map.of("question", question,
                                        "clarificationType", type,
                                        "clarificationId", clarificationId,
                                        "resumeThreadId", runConfig.threadId(),
                                        "resumeRunId", runConfig.runId())));
                        emitter.emit(event(seq, runConfig, AgentEventType.RUN_SUSPENDED,
                                "Run suspended waiting for user clarification.",
                                Map.of("question", question,
                                        "clarificationType", type,
                                        "clarificationId", clarificationId,
                                        "resumeThreadId", runConfig.threadId(),
                                        "resumeRunId", runConfig.runId())));
                        if (agentLoopRunStore != null) {
                            agentLoopRunStore.markSuspended(runConfig.runId(), "CLARIFICATION_REQUIRED");
                        }
                        stopped = true;
                        break;
                    }


                    // Persist raw tool call result for audit/debug
                    if (toolCallStore != null) {
                        try {
                            toolCallStore.saveResult(toolCall.id(), rawToolResult);
                        } catch (Exception e) {
                            log.warn("Failed to persist tool call result: {}", e.getMessage());
                        }
                    }

                    emitTodoMutationEventIfNeeded(seq, runConfig, emitter, pending, rawToolResult);

                    // Add raw history entry for observer source/evidence processing
                    history.add("Tool result (" + toolCall.toolName() + "): " + rawToolResult.result());

                    // Observer processes source/evidence (uses raw toolResult, does NOT compress)
                    String observation = observer.onToolCompleted(runConfig, toolCall, rawToolResult, events, seq, history);
                    emitter.flush();
                    if (observation != null && !observation.isBlank()) {
                        history.add(observation);
                    }

                    // Compress tool output BEFORE emitting TOOL_COMPLETED event and MessageStore persistence
                    String compressedResult = rawToolResult.result();
                    if (toolOutputBudgetMiddleware != null && rawToolResult.status() == ToolCallResult.Status.SUCCESS) {
                        String processed = toolOutputBudgetMiddleware.processToolOutput(
                                toolCall.toolName(), rawToolResult.result(), runConfig.threadId(), runConfig.runId(), null, null, null);
                        if (processed != null && processed.length() < rawToolResult.result().length()) {
                            compressedResult = processed;
                            int lastIdx = history.size() - 1;
                            if (lastIdx >= 0 && history.get(lastIdx).startsWith("Tool result (" + toolCall.toolName() + "): ")) {
                                history.set(lastIdx, "Tool result (" + toolCall.toolName() + "): " + compressedResult);
                            }
                            emitter.emit(event(seq, runConfig, AgentEventType.TOOL_OUTPUT_BUDGET_EXCEEDED,
                                    "Tool output budget exceeded for: " + toolCall.toolName() + ". Compressed.",
                                    Map.of("toolName", toolCall.toolName(), "compressed", true)));
                        }
                    }

                    // Emit TOOL_COMPLETED event with compressed content (ensures MessageStore/toolExecutionStore also see compressed)
                    String eventContent = compressedResult;
                    if (rawToolResult.status() == ToolCallResult.Status.FAILED && rawToolResult.error() != null) {
                        eventContent = rawToolResult.error();
                    }

                    Map<String, Object> completionMetadata = new HashMap<>(rawToolResult.metadata());
                    completionMetadata.put("toolCallId", toolCall.id());
                    completionMetadata.put("toolName", toolCall.toolName());
                    completionMetadata.put("status", rawToolResult.status().name());
                    completionMetadata.put("durationMs", toolDuration);
                    completionMetadata.put("error", rawToolResult.error() != null ? rawToolResult.error() : "");

                    if ("task".equals(pending.targetToolName())) {
                        Object subagentStatus = completionMetadata.getOrDefault("subagentStatus", rawToolResult.status().name());
                        emitter.emit(event(seq, runConfig, AgentEventType.SUBAGENT_COMPLETED,
                                "COMPLETED".equals(subagentStatus) || "SUCCESS".equals(subagentStatus)
                                        ? "Subagent task completed"
                                        : "Subagent task failed",
                                completionMetadata));
                    }

                    emitter.emit(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                            eventContent,
                            completionMetadata));
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
        }).subscribeOn(Schedulers.boundedElastic());
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
            return ToolCallResult.fromError(toolCall, "Tool not found: " + toolName, 0);
        }

        ToolRequest request = new ToolRequest(toolCall.arguments(), runConfig.workspaceRoot(),
                uploadedFileIds == null ? List.of() : uploadedFileIds, runConfig.threadId(), runConfig.runId(),
                runConfig.mode(), activeSkills);
        long startTime = System.currentTimeMillis();
        try {
            ToolResult result = tool.execute(request);
            long duration = System.currentTimeMillis() - startTime;
            return new ToolCallResult(toolCall.id(), toolCall.toolName(), toolCall.arguments(),
                    ToolCallResult.Status.SUCCESS, result.content(), "", duration, result.metadata());
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

    private static AgentEvent event(AtomicInteger seq, AgentRunConfig config, AgentEventType type, String content,
            Map<String, Object> metadata) {
        return AgentEvent.of(Integer.toString(seq.incrementAndGet()), config.runId(), config.threadId(), type, content,
                metadata);
    }

    private static void emitFinalAnswerRejected(AtomicInteger seq, AgentRunConfig runConfig, EventEmitter emitter,
            List<String> history, FinalAnswerDecision decision, int step, int totalToolCalls) {
        String retryInstruction = decision.retryInstruction() == null || decision.retryInstruction().isBlank()
                ? "Do not finish yet. Continue the pending todo work."
                : decision.retryInstruction();
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
}
