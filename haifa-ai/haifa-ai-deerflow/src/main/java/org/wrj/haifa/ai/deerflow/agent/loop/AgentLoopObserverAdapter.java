package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.AgentExecutionHook;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.CompletionDecision;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.CompletionResult;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionToolCall;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionToolResult;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ToolCallFilterResult;

/**
 * Temporary phase-62 adapter. Dependency direction is Legacy observer -> neutral execution hook;
 * Graph never imports this class or any other legacy loop type.
 */
public final class AgentLoopObserverAdapter implements AgentExecutionHook {
    private final AgentLoopObserver delegate;

    public AgentLoopObserverAdapter(AgentLoopObserver delegate) {
        this.delegate = delegate == null ? new NoopAgentLoopObserver() : delegate;
    }

    @Override
    public List<ToolCallFilterResult> filterToolCalls(AgentRunConfig config, List<ExecutionToolCall> calls) {
        List<ToolCall> legacy = calls.stream().map(AgentLoopObserverAdapter::legacyCall).toList();
        return delegate.afterToolCallsParsed(config, legacy).stream()
                .map(item -> new ToolCallFilterResult(neutralCall(item.toolCall()), item.allowed(), item.reason()))
                .toList();
    }

    @Override
    public ExecutionToolResult beforeTool(AgentRunConfig config, ExecutionToolCall call) {
        ToolCallResult result = delegate.beforeToolExecute(config, legacyCall(call));
        return result == null ? null : neutralResult(result);
    }

    @Override
    public String afterTool(AgentRunConfig config, ExecutionToolCall call, ExecutionToolResult result,
            List<AgentEvent> events, AtomicInteger sequence, List<String> history) {
        return delegate.onToolCompleted(config, legacyCall(call), legacyResult(result), events, sequence, history);
    }

    @Override
    public void afterStep(AgentRunConfig config, List<AgentEvent> events, AtomicInteger sequence, int step) {
        delegate.onStepCompleted(config, events, sequence, step);
    }

    @Override
    public boolean shouldContinue(AgentRunConfig config, String content, List<AgentEvent> events,
            AtomicInteger sequence, int step, int totalToolCalls, List<String> history) {
        return delegate.shouldContinue(config, content, events, sequence, step, totalToolCalls, history);
    }

    @Override
    public CompletionDecision evaluateFinalAnswer(AgentRunConfig config, String answer, List<AgentEvent> events,
            AtomicInteger sequence, int step, int totalToolCalls) {
        FinalAnswerDecision decision = delegate.onFinalAnswerProposed(
                config, answer, events, sequence, step, totalToolCalls);
        return new CompletionDecision(decision.accepted(), decision.answer(), decision.retryInstruction(), decision.metadata());
    }

    @Override
    public CompletionResult completeFinalAnswer(AgentRunConfig config, String answer, List<AgentEvent> events,
            AtomicInteger sequence, int step, int totalToolCalls) {
        FinalAnswerResult result = delegate.onFinalAnswerAccepted(
                config, answer, events, sequence, step, totalToolCalls);
        return new CompletionResult(result.finalAnswer(), result.extraMetadata());
    }

    @Override
    public void onMaxSteps(AgentRunConfig config, String content, List<AgentEvent> events,
            AtomicInteger sequence, int step, int totalToolCalls) {
        delegate.onMaxStepsReached(config, content, events, sequence, step, totalToolCalls);
    }

    private static ToolCall legacyCall(ExecutionToolCall call) {
        return new ToolCall(call.id(), call.toolName(), call.arguments(), ToolCall.Status.PENDING, call.metadata());
    }

    private static ExecutionToolCall neutralCall(ToolCall call) {
        return new ExecutionToolCall(call.id(), call.toolName(), call.arguments(), call.metadata());
    }

    private static ExecutionToolResult neutralResult(ToolCallResult result) {
        return new ExecutionToolResult(result.id(), result.toolName(), result.arguments(),
                ExecutionToolResult.Status.valueOf(result.status().name()),
                result.result(), result.error(), result.durationMs(), result.metadata());
    }

    private static ToolCallResult legacyResult(ExecutionToolResult result) {
        ToolCallResult.Status status;
        try {
            status = ToolCallResult.Status.valueOf(result.status().name());
        }
        catch (RuntimeException ignored) {
            status = ToolCallResult.Status.FAILED;
        }
        return new ToolCallResult(result.id(), result.toolName(), result.arguments(), status, result.result(),
                result.error(), result.durationMs(), result.metadata());
    }
}
