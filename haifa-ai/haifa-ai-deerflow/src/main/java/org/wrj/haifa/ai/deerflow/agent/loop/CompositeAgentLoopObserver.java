package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;

/**
 * Composite implementation of {@link AgentLoopObserver} that chains multiple observers.
 */
public class CompositeAgentLoopObserver implements AgentLoopObserver {

    private final List<AgentLoopObserver> observers;

    public CompositeAgentLoopObserver(List<AgentLoopObserver> observers) {
        this.observers = observers != null ? List.copyOf(observers) : List.of();
    }

    @Override
    public String onToolCompleted(AgentRunConfig runConfig, ToolCall toolCall, ToolCallResult toolResult,
                                  List<AgentEvent> events, AtomicInteger seq, List<String> history) {
        String lastObservation = null;
        for (AgentLoopObserver observer : observers) {
            String obs = observer.onToolCompleted(runConfig, toolCall, toolResult, events, seq, history);
            if (obs != null) {
                lastObservation = obs;
            }
        }
        return lastObservation;
    }

    @Override
    public void onStepCompleted(AgentRunConfig runConfig, List<AgentEvent> events, AtomicInteger seq, int step) {
        for (AgentLoopObserver observer : observers) {
            observer.onStepCompleted(runConfig, events, seq, step);
        }
    }

    @Override
    public boolean shouldContinue(AgentRunConfig runConfig, String responseContent, List<AgentEvent> events,
                                  AtomicInteger seq, int step, int totalToolCalls, List<String> history) {
        boolean cont = false;
        for (AgentLoopObserver observer : observers) {
            if (observer.shouldContinue(runConfig, responseContent, events, seq, step, totalToolCalls, history)) {
                cont = true;
            }
        }
        return cont;
    }

    @Override
    public FinalAnswerDecision onFinalAnswerProposed(AgentRunConfig runConfig, String rawAnswer, List<AgentEvent> events,
                                                     AtomicInteger seq, int step, int totalToolCalls) {
        String answer = rawAnswer;
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        FinalAnswerDecision firstRejection = null;
        for (AgentLoopObserver observer : observers) {
            FinalAnswerDecision decision = observer.onFinalAnswerProposed(runConfig, answer, events, seq, step, totalToolCalls);
            if (decision == null) {
                continue;
            }
            if (decision.metadata() != null) {
                metadata.putAll(decision.metadata());
            }
            if (!decision.accepted()) {
                if (firstRejection == null) {
                    firstRejection = decision;
                }
                continue;
            }
            answer = decision.answer();
        }
        if (firstRejection != null) {
            return FinalAnswerDecision.reject(firstRejection.retryInstruction(), metadata);
        }
        return FinalAnswerDecision.accept(answer, metadata);
    }

    @Override
    public FinalAnswerResult onFinalAnswerAccepted(AgentRunConfig runConfig, String rawAnswer, List<AgentEvent> events,
                                                   AtomicInteger seq, int step, int totalToolCalls) {
        String answer = rawAnswer;
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        for (AgentLoopObserver observer : observers) {
            FinalAnswerResult res = observer.onFinalAnswerAccepted(runConfig, answer, events, seq, step, totalToolCalls);
            if (res != null) {
                if (res.finalAnswer() != null) {
                    answer = res.finalAnswer();
                }
                if (res.extraMetadata() != null) {
                    metadata.putAll(res.extraMetadata());
                }
            }
        }
        return new FinalAnswerResult(answer, metadata);
    }

    @Override
    public void onMaxStepsReached(AgentRunConfig runConfig, String lastModelContent, List<AgentEvent> events,
                                  AtomicInteger seq, int step, int totalToolCalls) {
        for (AgentLoopObserver observer : observers) {
            observer.onMaxStepsReached(runConfig, lastModelContent, events, seq, step, totalToolCalls);
        }
    }

    @Override
    public ToolCallResult beforeToolExecute(AgentRunConfig runConfig, ToolCall toolCall) {
        for (AgentLoopObserver observer : observers) {
            ToolCallResult res = observer.beforeToolExecute(runConfig, toolCall);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    @Override
    public List<FilteredToolCall> afterToolCallsParsed(AgentRunConfig runConfig, List<ToolCall> toolCalls) {
        List<FilteredToolCall> current = toolCalls.stream()
                .map(tc -> new FilteredToolCall(tc, true, null))
                .toList();
        for (AgentLoopObserver observer : observers) {
            // Apply observer only to the currently allowed tool calls
            List<ToolCall> allowedCalls = current.stream()
                    .filter(FilteredToolCall::allowed)
                    .map(FilteredToolCall::toolCall)
                    .toList();
            List<FilteredToolCall> filtered = observer.afterToolCallsParsed(runConfig, allowedCalls);

            List<FilteredToolCall> next = new ArrayList<>();
            int filteredIdx = 0;
            for (FilteredToolCall ptc : current) {
                if (!ptc.allowed()) {
                    next.add(ptc);
                } else {
                    next.add(filtered.get(filteredIdx++));
                }
            }
            current = next;
        }
        return current;
    }
}
