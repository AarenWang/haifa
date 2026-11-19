package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.Map;

/**
 * Decision returned before a model final answer is accepted by the loop.
 */
public record FinalAnswerDecision(
        boolean accepted,
        String answer,
        String retryInstruction,
        Map<String, Object> metadata
) {

    public FinalAnswerDecision {
        answer = answer == null ? "" : answer;
        retryInstruction = retryInstruction == null ? "" : retryInstruction;
        metadata = metadata == null ? Map.of() : metadata;
    }

    public static FinalAnswerDecision accept(String answer, Map<String, Object> metadata) {
        return new FinalAnswerDecision(true, answer, "", metadata);
    }

    public static FinalAnswerDecision reject(String retryInstruction, Map<String, Object> metadata) {
        return new FinalAnswerDecision(false, "", retryInstruction, metadata);
    }
}
