package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;

/**
 * Generic observer interface for agent loop execution lifecycle.
 */
public interface AgentLoopObserver {

    /**
     * Invoked after a tool execution completes. Returns an optional observation string
     * to be appended to the agent's dialog history.
     */
    String onToolCompleted(AgentRunConfig runConfig, ToolCall toolCall, ToolCallResult toolResult,
            List<AgentEvent> events, AtomicInteger seq, List<String> history);

    /**
     * Invoked at the end of each agent loop iteration (step).
     */
    void onStepCompleted(AgentRunConfig runConfig, List<AgentEvent> events, AtomicInteger seq, int step);

    /**
     * Invoked when the model returns a final answer or no tool calls.
     * Returns true to force-continue the loop, appending any instructions to history.
     */
    boolean shouldContinue(AgentRunConfig runConfig, String responseContent, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls, List<String> history);

    /**
     * Invoked before a final answer is accepted. Return a rejection to append
     * a retry instruction to history and continue the loop.
     */
    default FinalAnswerDecision onFinalAnswerProposed(AgentRunConfig runConfig, String rawAnswer, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls) {
        return FinalAnswerDecision.accept(rawAnswer, Map.of());
    }

    /**
     * Invoked when the loop terminates with an accepted final answer.
     * Returns the finalized answer text along with any additional metadata.
     */
    FinalAnswerResult onFinalAnswerAccepted(AgentRunConfig runConfig, String rawAnswer, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls);

    /**
     * Invoked when the loop exhausts its maximum step limit.
     */
    void onMaxStepsReached(AgentRunConfig runConfig, String lastModelContent, List<AgentEvent> events,
            AtomicInteger seq, int step, int totalToolCalls);

    /**
     * Invoked before a tool is executed. Returns a pre-computed ToolCallResult
     * if execution should be bypassed (e.g. cached results), or null to proceed normally.
     */
    default ToolCallResult beforeToolExecute(AgentRunConfig runConfig, ToolCall toolCall) {
        return null;
    }

    /**
     * Invoked after tool calls are parsed from a model response but before any are executed.
     * Allows observers to filter, reject, or modify the tool call list.
     * Each returned entry contains the (possibly modified) tool call and a flag indicating
     * whether it should be executed. Rejected tool calls are automatically emitted as
     * TOOL_COMPLETED events with the provided reason.
     *
     * @return list of filtered tool call entries
     */
    default List<FilteredToolCall> afterToolCallsParsed(AgentRunConfig runConfig, List<ToolCall> toolCalls) {
        return toolCalls.stream().map(tc -> new FilteredToolCall(tc, true, null)).toList();
    }

    /**
     * Record representing a tool call after filtering.
     */
    record FilteredToolCall(ToolCall toolCall, boolean allowed, String reason) {}
}
