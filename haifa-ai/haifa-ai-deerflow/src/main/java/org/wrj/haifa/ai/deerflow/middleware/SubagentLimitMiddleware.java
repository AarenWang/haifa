package org.wrj.haifa.ai.deerflow.middleware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoopObserver;
import org.wrj.haifa.ai.deerflow.agent.loop.FinalAnswerResult;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.subagent.SubagentRuntime;

/**
 * Middleware that enforces concurrency limits on subagent {@code task} tool calls.
 *
 * <p>Limits applied (server-side, NOT prompt-only):
 * <ul>
 *   <li>Single model response: max {@code maxPerResponse} {@code task} calls</li>
 *   <li>Same parent run: max {@code maxConcurrent} simultaneously running subagents</li>
 *   <li>Subagent internal: subagents do NOT have access to {@code task} (recursive nesting blocked by tool filtering)</li>
 * </ul>
 *
 * <p>Exceeded calls are rejected with a structured message returned as the tool result.
 * Token usage and failure reasons are tracked per run.
 */
@Component
public class SubagentLimitMiddleware implements AgentLoopObserver {

    private static final Logger log = LoggerFactory.getLogger(SubagentLimitMiddleware.class);

    private final int maxPerResponse;
    private final int maxConcurrent;
    private final SubagentRuntime subagentRuntime;

    // Track rejection counts per run for observability
    private final ConcurrentHashMap<String, AtomicInteger> rejectionCounts = new ConcurrentHashMap<>();

    public SubagentLimitMiddleware(DeerFlowProperties properties, SubagentRuntime subagentRuntime) {
        this.maxPerResponse = properties.getSubagentMaxPerResponse() > 0
                ? properties.getSubagentMaxPerResponse() : 3;
        this.maxConcurrent = properties.getSubagentMaxConcurrent() > 0
                ? properties.getSubagentMaxConcurrent() : 3;
        this.subagentRuntime = subagentRuntime;
    }

    @Override
    public List<FilteredToolCall> afterToolCallsParsed(org.wrj.haifa.ai.deerflow.agent.AgentRunConfig runConfig,
                                                        List<ToolCall> toolCalls) {
        long startedAt = System.nanoTime();
        String runId = runConfig.runId();
        log.info("layer=middleware phase=enter middleware=SubagentLimitMiddleware method=afterToolCallsParsed runId={} threadId={} toolCallCount={}",
                runId, runConfig.threadId(), toolCalls == null ? 0 : toolCalls.size());
        try {
            return filterToolCalls(runId, toolCalls);
        } finally {
            log.info("layer=middleware phase=exit middleware=SubagentLimitMiddleware method=afterToolCallsParsed durationMs={} runId={} threadId={}",
                    (System.nanoTime() - startedAt) / 1_000_000, runId, runConfig.threadId());
        }
    }

    private List<FilteredToolCall> filterToolCalls(String runId, List<ToolCall> toolCalls) {
        List<FilteredToolCall> result = new ArrayList<>();
        int taskCount = 0;
        int activeCount = subagentRuntime != null ? subagentRuntime.activeCount(runId) : 0;

        for (ToolCall tc : toolCalls) {
            if (!"task".equals(tc.toolName())) {
                result.add(new FilteredToolCall(tc, true, null));
                continue;
            }

            // A 4xx provider failure is configuration/request-shape related, so retrying task
            // dispatch in the same parent run cannot make progress and used to cause retry loops.
            if (subagentRuntime != null && subagentRuntime.hasProviderConfigurationFailure(runId)) {
                String reason = subagentRuntime.providerConfigurationFailureReason(runId);
                log.warn("layer=middleware SubagentLimitMiddleware: rejecting task call {} for run {}. {}",
                        tc.id(), runId, reason);
                incrementRejections(runId);
                result.add(new FilteredToolCall(tc, false, reason));
                continue;
            }

            // Check per-response limit
            taskCount++;
            if (taskCount > maxPerResponse) {
                log.warn("layer=middleware SubagentLimitMiddleware: rejecting task call {} for run {}. "
                        + "Exceeded per-response limit ({}/{})",
                        tc.id(), runId, taskCount, maxPerResponse);
                incrementRejections(runId);
                result.add(new FilteredToolCall(tc, false,
                        "Subagent concurrency limit exceeded: max " + maxPerResponse
                                + " task calls per response. This call was discarded. "
                                + "Batch your sub-tasks across multiple turns."));
                continue;
            }

            // Check concurrent limit (including this one)
            if (activeCount + taskCount > maxConcurrent) {
                log.warn("layer=middleware SubagentLimitMiddleware: rejecting task call {} for run {}. "
                        + "Exceeded concurrent limit (active={}, new={}, max={})",
                        tc.id(), runId, activeCount, taskCount, maxConcurrent);
                incrementRejections(runId);
                result.add(new FilteredToolCall(tc, false,
                        "Subagent concurrency limit exceeded: max " + maxConcurrent
                                + " concurrent subagents per run. Current active: " + activeCount
                                + ". Wait for existing subagents to complete before launching more."));
                continue;
            }

            result.add(new FilteredToolCall(tc, true, null));
        }
        return result;
    }

    @Override
    public String onToolCompleted(org.wrj.haifa.ai.deerflow.agent.AgentRunConfig runConfig, ToolCall toolCall,
                                   org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult toolResult,
                                   List<org.wrj.haifa.ai.deerflow.agent.AgentEvent> events,
                                   AtomicInteger seq, List<String> history) {
        return null; // No-op; we only filter before execution
    }

    @Override
    public void onStepCompleted(org.wrj.haifa.ai.deerflow.agent.AgentRunConfig runConfig,
                                 List<org.wrj.haifa.ai.deerflow.agent.AgentEvent> events,
                                 AtomicInteger seq, int step) {
        // No-op
    }

    @Override
    public boolean shouldContinue(org.wrj.haifa.ai.deerflow.agent.AgentRunConfig runConfig, String responseContent,
                                 List<org.wrj.haifa.ai.deerflow.agent.AgentEvent> events,
                                 AtomicInteger seq, int step, int totalToolCalls, List<String> history) {
        return false;
    }

    @Override
    public FinalAnswerResult onFinalAnswerAccepted(org.wrj.haifa.ai.deerflow.agent.AgentRunConfig runConfig,
                                                     String rawAnswer,
                                                     List<org.wrj.haifa.ai.deerflow.agent.AgentEvent> events,
                                                     AtomicInteger seq, int step, int totalToolCalls) {
        return new FinalAnswerResult(rawAnswer, Map.of());
    }

    @Override
    public void onMaxStepsReached(org.wrj.haifa.ai.deerflow.agent.AgentRunConfig runConfig, String lastModelContent,
                                   List<org.wrj.haifa.ai.deerflow.agent.AgentEvent> events,
                                   AtomicInteger seq, int step, int totalToolCalls) {
        // No-op
    }

    public int getRejectionCount(String runId) {
        AtomicInteger counter = rejectionCounts.get(runId);
        return counter == null ? 0 : counter.get();
    }

    private void incrementRejections(String runId) {
        rejectionCounts.computeIfAbsent(runId, k -> new AtomicInteger(0)).incrementAndGet();
    }
}
