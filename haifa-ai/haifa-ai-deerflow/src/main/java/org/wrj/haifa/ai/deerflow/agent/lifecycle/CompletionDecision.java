package org.wrj.haifa.ai.deerflow.agent.lifecycle;

import java.util.Map;

public record CompletionDecision(boolean accepted, String answer, String retryInstruction,
                                 Map<String, Object> metadata) {
    public CompletionDecision {
        answer = answer == null ? "" : answer;
        retryInstruction = retryInstruction == null ? "" : retryInstruction;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static CompletionDecision accept(String answer, Map<String, Object> metadata) {
        return new CompletionDecision(true, answer, "", metadata);
    }

    public static CompletionDecision reject(String instruction, Map<String, Object> metadata) {
        return new CompletionDecision(false, "", instruction, metadata);
    }
}
