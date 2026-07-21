package org.wrj.haifa.ai.deerflow.agent.lifecycle;

import java.util.Map;

public record CompletionResult(String finalAnswer, Map<String, Object> metadata) {
    public CompletionResult {
        finalAnswer = finalAnswer == null ? "" : finalAnswer;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
