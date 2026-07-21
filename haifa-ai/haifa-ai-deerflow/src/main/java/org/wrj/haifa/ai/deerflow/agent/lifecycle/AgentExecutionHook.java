package org.wrj.haifa.ai.deerflow.agent.lifecycle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;

/** Graph-neutral lifecycle extension contract. */
public interface AgentExecutionHook {
    default List<ToolCallFilterResult> filterToolCalls(AgentRunConfig config, List<ExecutionToolCall> calls) {
        return calls.stream().map(call -> new ToolCallFilterResult(call, true, null)).toList();
    }

    default ExecutionToolResult beforeTool(AgentRunConfig config, ExecutionToolCall call) { return null; }

    default String afterTool(AgentRunConfig config, ExecutionToolCall call, ExecutionToolResult result,
            List<AgentEvent> events, AtomicInteger sequence, List<String> history) { return null; }

    default void afterStep(AgentRunConfig config, List<AgentEvent> events, AtomicInteger sequence, int step) { }

    default boolean shouldContinue(AgentRunConfig config, String content, List<AgentEvent> events,
            AtomicInteger sequence, int step, int totalToolCalls, List<String> history) { return false; }

    default CompletionDecision evaluateFinalAnswer(AgentRunConfig config, String answer, List<AgentEvent> events,
            AtomicInteger sequence, int step, int totalToolCalls) {
        return CompletionDecision.accept(answer, Map.of());
    }

    default CompletionResult completeFinalAnswer(AgentRunConfig config, String answer, List<AgentEvent> events,
            AtomicInteger sequence, int step, int totalToolCalls) {
        return new CompletionResult(answer, Map.of());
    }

    default void onMaxSteps(AgentRunConfig config, String content, List<AgentEvent> events,
            AtomicInteger sequence, int step, int totalToolCalls) { }
}
