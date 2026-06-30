package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentLoopRunEntity;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ModelStepStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Core agent loop that supports multi-turn model calls and tool execution.
 *
 * Loop flow:
 * 1. Build initial prompt with user message and available tools.
 * 2. Call model.
 * 3. Parse response for tool_call requests.
 * 4. Execute requested tools (with policy check, thread/upload context), emit TOOL_STARTED/TOOL_COMPLETED events.
 * 5. Append tool results to message history.
 * 6. Call model again with updated context.
 * 7. Detect final_answer or stop conditions (max steps, max tool calls, timeout).
 */
public class AgentLoop {

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);

    private final AgentModelClient modelClient;
    private final ToolRegistry toolRegistry;
    private final ToolCallParser toolCallParser;
    private final ModelStepStore modelStepStore;
    private final ToolCallStore toolCallStore;
    private final AgentLoopRunStore agentLoopRunStore;

    public AgentLoop(AgentModelClient modelClient, ToolRegistry toolRegistry) {
        this(modelClient, toolRegistry, null, null, null);
    }

    public AgentLoop(AgentModelClient modelClient, ToolRegistry toolRegistry,
            ModelStepStore modelStepStore, ToolCallStore toolCallStore, AgentLoopRunStore agentLoopRunStore) {
        this.modelClient = modelClient;
        this.toolRegistry = toolRegistry;
        this.toolCallParser = new ToolCallParser();
        this.modelStepStore = modelStepStore;
        this.toolCallStore = toolCallStore;
        this.agentLoopRunStore = agentLoopRunStore;
    }

    /**
     * Execute the research loop and return a stream of events.
     *
     * @param config        loop configuration
     * @param runConfig     agent run config (threadId, runId, etc.)
     * @param systemPrompt  system prompt for the model
     * @param userMessage   initial user message
     * @param seq           event sequence counter
     * @param toolPolicy    optional tool policy service (may be null)
     * @param activeSkills  optional active skills for policy check (may be null)
     * @param uploadedFileIds optional uploaded file IDs for tool context (may be null)
     * @return Flux of AgentEvent
     */
    public Flux<AgentEvent> run(
            LoopConfig config,
            AgentRunConfig runConfig,
            String systemPrompt,
            String userMessage,
            AtomicInteger seq,
            ToolPolicyService toolPolicy,
            List<Skill> activeSkills,
            List<String> uploadedFileIds) {
        return Flux.defer(() -> {
            long loopStartTime = System.currentTimeMillis();
            List<String> history = new ArrayList<>();
            history.add("User: " + userMessage);

            // Persist loop run start
            if (agentLoopRunStore != null) {
                agentLoopRunStore.create(runConfig.runId(), runConfig.threadId(), config);
            }

            StringBuilder toolDescriptions = new StringBuilder();
            for (AgentTool tool : toolRegistry.tools()) {
                String toolName = tool.name();
                if (toolPolicy != null && !toolPolicy.isToolAllowed(toolName, activeSkills, runConfig.mode())) {
                    continue;
                }
                toolDescriptions.append("- ").append(toolName).append(": ").append(tool.description()).append("\n");
            }

            String fullSystemPrompt = systemPrompt + "\n\nAvailable tools:\n" + toolDescriptions
                    + "\nWhen you need to use a tool, emit: <tool_call name=\"tool_name\">{\"arg\":\"value\"}</tool_call>\n"
                    + "When you have enough information, provide your final answer starting with <final_answer>.";

            List<AgentEvent> events = new ArrayList<>();
            int totalToolCalls = 0;
            boolean stopped = false;
            String stopReason = null;

            for (int step = 0; step < config.maxSteps(); step++) {
                if (agentLoopRunStore != null) {
                    agentLoopRunStore.updateStepCount(runConfig.runId(), step + 1);
                }

                // Check timeout
                if (System.currentTimeMillis() - loopStartTime > config.timeoutMs()) {
                    stopReason = "TIMEOUT";
                    events.add(event(seq, runConfig, AgentEventType.RUN_FAILED,
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
                    events.add(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
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
                events.add(event(seq, runConfig, AgentEventType.MODEL_STARTED,
                        "Model step " + (step + 1) + "/" + config.maxSteps(),
                        Map.of("step", step + 1, "maxSteps", config.maxSteps())));

                ModelPrompt prompt = new ModelPrompt(fullSystemPrompt, userPrompt, runConfig.modelName());
                ModelResponse modelResponse;
                try {
                    modelResponse = modelClient.generate(prompt).block();
                } catch (Exception ex) {
                    log.error("Model call failed at step {}. runId={}", step, runConfig.runId(), ex);
                    stopReason = "ERROR";
                    events.add(event(seq, runConfig, AgentEventType.RUN_FAILED,
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
                events.add(event(seq, runConfig, AgentEventType.MODEL_DELTA,
                        modelResponse.content(),
                        Map.of("step", step + 1, "modelDurationMs", modelDuration)));

                // Check for final answer
                if (toolCallParser.hasFinalAnswer(modelResponse.content())) {
                    String finalAnswer = toolCallParser.extractFinalAnswer(modelResponse.content());
                    stopReason = "FINAL_ANSWER";
                    events.add(event(seq, runConfig, AgentEventType.MODEL_COMPLETED,
                            finalAnswer,
                            Map.of("step", step + 1, "modelDurationMs", modelDuration, "stopReason", stopReason)));
                    events.add(event(seq, runConfig, AgentEventType.RESEARCH_STEP_COMPLETED,
                            "Research completed after " + (step + 1) + " steps",
                            Map.of("steps", step + 1, "totalToolCalls", totalToolCalls, "stopReason", stopReason)));
                    events.add(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
                            "Research run completed",
                            Map.of("stopReason", stopReason, "steps", step + 1, "totalToolCalls", totalToolCalls)));
                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markCompleted(runConfig.runId(), stopReason);
                    }
                    stopped = true;
                    break;
                }

                // Extract tool calls (prioritize structured tool calls, fallback to text tags)
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

                // Process invalid tool calls explicitly so they are not swallowed silently
                if (modelResponse.invalidToolCalls() != null && !modelResponse.invalidToolCalls().isEmpty()) {
                    for (String invalidCall : modelResponse.invalidToolCalls()) {
                        String invalidCallId = java.util.UUID.randomUUID().toString();
                        events.add(event(seq, runConfig, AgentEventType.TOOL_CALL_REQUESTED,
                                "Invalid tool call: " + invalidCall,
                                Map.of("toolCallId", invalidCallId, "error", "Invalid tool call format or arguments")));
                        events.add(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                                "Error: Invalid tool call arguments",
                                Map.of("toolCallId", invalidCallId, "status", "FAILED", "error", "Invalid tool call arguments")));
                        history.add("Tool result (error): Invalid tool call arguments for " + invalidCall);
                    }
                }

                if (loopToolCalls.isEmpty()) {
                    // No tool calls, no final answer — treat as completed
                    stopReason = runConfig.mode() == org.wrj.haifa.ai.deerflow.agent.RunMode.CHAT ? "ASSISTANT_COMPLETED" : "NO_TOOL_CALLS";
                    String completionMsg = runConfig.mode() == org.wrj.haifa.ai.deerflow.agent.RunMode.CHAT 
                        ? "Chat completed" 
                        : "Research completed after " + (step + 1) + " steps (no tool calls)";

                    events.add(event(seq, runConfig, AgentEventType.MODEL_COMPLETED,
                            modelResponse.content(),
                            Map.of("step", step + 1, "modelDurationMs", modelDuration, "stopReason", stopReason)));
                    events.add(event(seq, runConfig, AgentEventType.RESEARCH_STEP_COMPLETED,
                            completionMsg,
                            Map.of("steps", step + 1, "totalToolCalls", totalToolCalls, "stopReason", stopReason)));
                    events.add(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
                            "Run completed",
                            Map.of("stopReason", stopReason, "steps", step + 1, "totalToolCalls", totalToolCalls)));
                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markCompleted(runConfig.runId(), stopReason);
                    }
                    stopped = true;
                    break;
                }

                // Execute tool calls
                List<ToolCallResult> stepToolResults = new ArrayList<>();
                for (ToolCall toolCall : loopToolCalls) {
                    totalToolCalls++;
                    if (totalToolCalls > config.maxToolCalls()) {
                        break;
                    }

                    events.add(event(seq, runConfig, AgentEventType.TOOL_CALL_REQUESTED,
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
                    if (toolPolicy != null && activeSkills != null && !toolPolicy.isToolAllowed(targetToolName, activeSkills, runConfig.mode())) {
                        events.add(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                                "Policy denied " + targetToolName,
                                Map.of("toolCallId", toolCall.id(), "toolName", targetToolName, "denied", true)));
                        events.add(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                                "Tool denied by policy",
                                Map.of("toolCallId", toolCall.id(), "toolName", targetToolName, "denied", true)));
                        history.add("Tool result (" + targetToolName + "): Tool denied by policy");
                        continue;
                    }

                    long toolStartTime = System.currentTimeMillis();
                    events.add(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                            "Executing " + targetToolName,
                            Map.of("toolCallId", toolCall.id(), "toolName", targetToolName)));

                    ToolCallResult toolResult = executeTool(toolCall, runConfig, uploadedFileIds);
                    long toolDuration = System.currentTimeMillis() - toolStartTime;

                    // Persist tool call result
                    if (toolCallStore != null) {
                        try {
                            toolCallStore.saveResult(toolCall.id(), toolResult);
                        } catch (Exception e) {
                            log.warn("Failed to persist tool call result: {}", e.getMessage());
                        }
                    }

                    String eventContent = toolResult.result() != null ? toolResult.result() : "";
                    if (toolResult.status() == ToolCallResult.Status.FAILED && toolResult.error() != null) {
                        eventContent = toolResult.error();
                    }

                    events.add(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                            eventContent,
                            Map.of("toolCallId", toolCall.id(), "toolName", toolCall.toolName(),
                                    "status", toolResult.status().name(), "durationMs", toolDuration,
                                    "error", toolResult.error() != null ? toolResult.error() : "")));

                    history.add("Tool result (" + toolCall.toolName() + "): " + toolResult.result());
                    stepToolResults.add(toolResult);
                }

                if (totalToolCalls >= config.maxToolCalls()) {
                    stopReason = "MAX_TOOL_CALLS_REACHED";
                    events.add(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
                            "Max tool calls reached after " + totalToolCalls + " tool calls",
                            Map.of("stopReason", stopReason, "steps", step + 1, "totalToolCalls", totalToolCalls)));
                    if (agentLoopRunStore != null) {
                        agentLoopRunStore.markCompleted(runConfig.runId(), stopReason);
                    }
                    stopped = true;
                    break;
                }

                events.add(event(seq, runConfig, AgentEventType.RESEARCH_STEP_COMPLETED,
                        "Step " + (step + 1) + " completed",
                        Map.of("step", step + 1, "totalToolCalls", totalToolCalls)));
            }

            // If loop exhausted max steps without final answer
            if (!stopped) {
                stopReason = "MAX_STEPS_REACHED";
                events.add(event(seq, runConfig, AgentEventType.RUN_COMPLETED,
                        "Max steps reached without final answer",
                        Map.of("stopReason", stopReason, "maxSteps", config.maxSteps(), "totalToolCalls", totalToolCalls)));
                if (agentLoopRunStore != null) {
                    agentLoopRunStore.markCompleted(runConfig.runId(), stopReason);
                }
            }

            return Flux.fromIterable(events);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private ToolCallResult executeTool(ToolCall toolCall, AgentRunConfig runConfig, List<String> uploadedFileIds) {
        String toolName = toolCall.toolName();
        String args = toolCall.arguments();

        AgentTool tool = findTool(toolName);
        if (tool == null) {
            log.warn("Tool not found: {}. Available: {}", toolName, toolRegistry.tools().stream().map(AgentTool::name).toList());
            return ToolCallResult.fromError(toolCall, "Tool not found: " + toolName, 0);
        }

        ToolRequest request = new ToolRequest(args, runConfig.workspaceRoot(),
                uploadedFileIds == null ? List.of() : uploadedFileIds, runConfig.threadId());
        long startTime = System.currentTimeMillis();
        try {
            ToolResult result = tool.execute(request);
            long duration = System.currentTimeMillis() - startTime;
            return ToolCallResult.from(toolCall, result.content(), duration);
        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Tool {} failed: {}", toolName, ex.getMessage());
            return ToolCallResult.fromError(toolCall, "Tool failed: " + ex.getMessage(), duration);
        }
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
}
