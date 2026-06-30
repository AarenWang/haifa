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
                toolDescriptions.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
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
                String modelResponse;
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
                    modelResponse = "";
                }

                // Persist model step
                if (modelStepStore != null) {
                    try {
                        ModelStep modelStep = new ModelStep(step + 1, userPrompt, modelResponse, List.of(), modelStartTime, modelDuration);
                        modelStepStore.save(modelStep, runConfig.runId(), runConfig.threadId());
                    } catch (Exception e) {
                        log.warn("Failed to persist model step: {}", e.getMessage());
                    }
                }

                history.add("Assistant: " + modelResponse);
                events.add(event(seq, runConfig, AgentEventType.MODEL_DELTA,
                        modelResponse,
                        Map.of("step", step + 1, "modelDurationMs", modelDuration)));

                // Check for final answer
                if (toolCallParser.hasFinalAnswer(modelResponse)) {
                    String finalAnswer = toolCallParser.extractFinalAnswer(modelResponse);
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

                // Parse tool calls
                List<ToolCallParser.ParsedToolCall> parsedCalls = toolCallParser.parse(modelResponse);
                if (parsedCalls.isEmpty()) {
                    // No tool calls, no final answer — treat as completed
                    stopReason = "NO_TOOL_CALLS";
                    events.add(event(seq, runConfig, AgentEventType.MODEL_COMPLETED,
                            modelResponse,
                            Map.of("step", step + 1, "modelDurationMs", modelDuration, "stopReason", stopReason)));
                    events.add(event(seq, runConfig, AgentEventType.RESEARCH_STEP_COMPLETED,
                            "Research completed after " + (step + 1) + " steps (no tool calls)",
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

                // Execute tool calls
                List<ToolCallResult> stepToolResults = new ArrayList<>();
                for (ToolCallParser.ParsedToolCall parsedCall : parsedCalls) {
                    totalToolCalls++;
                    if (totalToolCalls > config.maxToolCalls()) {
                        break;
                    }

                    ToolCall toolCall = ToolCall.of(parsedCall.toolName(), parsedCall.arguments());
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

                    // Policy check
                    if (toolPolicy != null && activeSkills != null && !toolPolicy.isToolAllowed(toolCall.toolName(), activeSkills)) {
                        events.add(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                                "Policy denied " + toolCall.toolName(),
                                Map.of("toolCallId", toolCall.id(), "toolName", toolCall.toolName(), "denied", true)));
                        events.add(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                                "Tool denied by policy",
                                Map.of("toolCallId", toolCall.id(), "toolName", toolCall.toolName(), "denied", true)));
                        history.add("Tool result (" + toolCall.toolName() + "): Tool denied by policy");
                        continue;
                    }

                    long toolStartTime = System.currentTimeMillis();
                    events.add(event(seq, runConfig, AgentEventType.TOOL_STARTED,
                            "Executing " + toolCall.toolName(),
                            Map.of("toolCallId", toolCall.id(), "toolName", toolCall.toolName())));

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

                    events.add(event(seq, runConfig, AgentEventType.TOOL_COMPLETED,
                            toolResult.result(),
                            Map.of("toolCallId", toolCall.id(), "toolName", toolCall.toolName(),
                                    "status", toolResult.status().name(), "durationMs", toolDuration,
                                    "error", toolResult.error())));

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
        for (AgentTool tool : toolRegistry.tools()) {
            if (tool.name().equals(toolName)) {
                return tool;
            }
        }
        return null;
    }

    private static AgentEvent event(AtomicInteger seq, AgentRunConfig config, AgentEventType type, String content,
            Map<String, Object> metadata) {
        return AgentEvent.of(Integer.toString(seq.incrementAndGet()), config.runId(), config.threadId(), type, content,
                metadata);
    }
}
