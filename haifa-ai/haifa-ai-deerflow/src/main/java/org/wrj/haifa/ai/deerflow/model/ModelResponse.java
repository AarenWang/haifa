package org.wrj.haifa.ai.deerflow.model;

import java.util.List;
import java.util.Map;

/**
 * Structured response from the LLM client.
 */
public record ModelResponse(
    String content,
    List<ModelToolCall> toolCalls,
    List<String> invalidToolCalls,
    String finishReason,
    Map<String, Object> metadata
) {
    public ModelResponse(String content) {
        this(content, List.of(), List.of(), null, Map.of());
    }

    public ModelResponse(String content, List<ModelToolCall> toolCalls) {
        this(content, toolCalls, List.of(), null, Map.of());
    }
}
