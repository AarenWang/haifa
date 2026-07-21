package org.wrj.haifa.ai.deerflow.subagent;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionLimits;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.RunExecutionContext;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphChatRuntime;
import org.wrj.haifa.ai.deerflow.graph.GraphChatRuntimeRequest;
import org.wrj.haifa.ai.deerflow.run.RunCancellationService;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;

/** Executes child work through the same Lead Agent Graph used by top-level requests. */
@Component
public class SubgraphRunner {
    private final GraphChatRuntime graphRuntime;
    private final RunManager runManager;
    private final RunCancellationService cancellationService;
    private final MessageStore messageStore;
    private final DeerFlowProperties properties;
    private final ToolPolicyService toolPolicyService;

    public SubgraphRunner(GraphChatRuntime graphRuntime, RunManager runManager,
            RunCancellationService cancellationService, MessageStore messageStore,
            DeerFlowProperties properties, ToolPolicyService toolPolicyService) {
        this.graphRuntime = graphRuntime;
        this.runManager = runManager;
        this.cancellationService = cancellationService;
        this.messageStore = messageStore;
        this.properties = properties;
        this.toolPolicyService = toolPolicyService;
    }

    public SubagentResult execute(ChildRequest request, SubagentExecutionHook hook) {
        long started = System.currentTimeMillis();
        Map<String, Object> metadata = Map.of(
                "parentRunId", request.parentRunId(),
                "parentToolCallId", request.parentToolCallId(),
                "subagentType", request.subagentType(),
                "depth", request.depth(),
                "childRun", true);
        var child = runManager.create(request.threadId(), request.modelName(), metadata);
        runManager.markRunning(child.runId());
        cancellationService.registerChild(request.parentRunId(), child.runId());

        AgentRequest agentRequest = new AgentRequest(request.threadId(), request.prompt(), request.modelName(),
                List.of(), RunMode.CHAT, ResearchOptions.defaults(), "subagent", metadata);
        AgentRunConfig runConfig = new AgentRunConfig(request.threadId(), child.runId(), request.modelName(),
                false, false, request.maxTurns(), Path.of(properties.getWorkspaceRoot()), RunMode.CHAT,
                ResearchOptions.defaults(), metadata);
        RunExecutionContext context = new RunExecutionContext(runConfig, agentRequest,
                new ExecutionLimits(request.maxTurns(), Math.max(5, request.maxTurns() * 2),
                        request.timeoutMs(), ResearchOptions.defaults()),
                hook, new AtomicInteger(), toolPolicyService, request.skills(), List.of(),
                request.allowedToolNames(), request.parentRunId(), request.parentToolCallId(), request.depth());

        try {
            List<AgentEvent> events = graphRuntime.run(new GraphChatRuntimeRequest(context, List.of()))
                    .collectList().block(Duration.ofMillis(outerTimeoutMs(request.timeoutMs())));
            long duration = System.currentTimeMillis() - started;
            if (events == null) {
                return SubagentResult.failed(child.runId(), request.parentRunId(), "Child Graph produced no events", duration);
            }
            if (events.stream().anyMatch(event -> event.type() == AgentEventType.RUN_CANCELLED)) {
                return SubagentResult.cancelled(child.runId(), request.parentRunId(), "Child Graph cancelled", duration);
            }
            var failure = events.stream().filter(event -> event.type() == AgentEventType.RUN_FAILED).findFirst();
            if (failure.isPresent()) {
                return SubagentResult.failed(child.runId(), request.parentRunId(), failure.get().content(), duration);
            }
            return SubagentResult.success(child.runId(), request.parentRunId(), finalAnswer(events),
                    hook.evidenceIds(), hook.sourceIds(), tokenUsage(events), duration);
        }
        catch (Exception ex) {
            long duration = System.currentTimeMillis() - started;
            if (cancellationService.isCancelled(child.runId())) {
                return SubagentResult.cancelled(child.runId(), request.parentRunId(), ex.getMessage(), duration);
            }
            if (isTimeout(ex)) {
                cancellationService.requestCancel(child.runId(), "SUBAGENT_TIMEOUT");
                runManager.tryMarkFailed(child.runId(), "SUBAGENT_TIMEOUT");
                return SubagentResult.timedOut(child.runId(), request.parentRunId(),
                        "Child Graph timed out after " + request.timeoutMs() + " ms", duration);
            }
            runManager.tryMarkFailed(child.runId(), ex.getMessage());
            return SubagentResult.failed(child.runId(), request.parentRunId(), ex.getMessage(), duration);
        }
    }

    private static String finalAnswer(List<AgentEvent> events) {
        for (int i = events.size() - 1; i >= 0; i--) {
            AgentEvent event = events.get(i);
            if ((event.type() == AgentEventType.RUN_COMPLETED || event.type() == AgentEventType.MODEL_COMPLETED
                    || event.type() == AgentEventType.MODEL_DELTA)
                    && event.content() != null && !event.content().isBlank()) {
                return event.content();
            }
        }
        return "";
    }

    private static Map<String, Integer> tokenUsage(List<AgentEvent> events) {
        int chars = events.stream().map(AgentEvent::content).filter(java.util.Objects::nonNull)
                .collect(Collectors.summingInt(String::length));
        return Map.of("estimated_total_tokens", chars / 4);
    }

    public record ChildRequest(String parentRunId, String parentToolCallId, String threadId, String modelName,
                               String subagentType, String prompt, Set<String> allowedToolNames,
                               List<Skill> skills, int maxTurns, long timeoutMs, int depth) {
        public ChildRequest {
            allowedToolNames = allowedToolNames == null ? Set.of() : Set.copyOf(allowedToolNames);
            skills = skills == null ? List.of() : List.copyOf(skills);
            maxTurns = Math.max(1, maxTurns);
            timeoutMs = Math.max(1, timeoutMs);
            depth = Math.max(1, depth);
        }
    }

    private static boolean isTimeout(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof java.util.concurrent.TimeoutException
                    || (current.getMessage() != null
                    && current.getMessage().toLowerCase(java.util.Locale.ROOT).contains("timeout"))) {
                return true;
            }
        }
        return false;
    }

    private static long outerTimeoutMs(long executionTimeoutMs) {
        long completionGraceMs = Math.min(5_000, Math.max(100, executionTimeoutMs / 10));
        return executionTimeoutMs + completionGraceMs;
    }
}
