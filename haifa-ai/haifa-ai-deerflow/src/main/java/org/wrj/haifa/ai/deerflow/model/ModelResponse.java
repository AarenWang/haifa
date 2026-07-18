package org.wrj.haifa.ai.deerflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
import org.wrj.haifa.ai.deerflow.model.cache.ModelUsage;

/**
 * Structured response from the LLM client.
 */
public record ModelResponse(
    String content,
    List<ModelToolCall> toolCalls,
    List<String> invalidToolCalls,
    String finishReason,
    Map<String, Object> metadata,
    @JsonIgnore ModelProtocolState protocolState,
    ModelUsage usage
) {
    public ModelResponse {
        content = content == null ? "" : content;
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        invalidToolCalls = invalidToolCalls == null ? List.of() : List.copyOf(invalidToolCalls);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        protocolState = protocolState == null ? ModelProtocolState.empty() : protocolState;
        usage = usage == null ? ModelUsage.empty() : usage;
    }

    public ModelResponse(String content) {
        this(content, List.of(), List.of(), null, Map.of(), ModelProtocolState.empty(), ModelUsage.empty());
    }

    public ModelResponse(String content, List<ModelToolCall> toolCalls) {
        this(content, toolCalls, List.of(), null, Map.of(), ModelProtocolState.empty(), ModelUsage.empty());
    }

    public ModelResponse(String content, List<ModelToolCall> toolCalls, List<String> invalidToolCalls, String finishReason, Map<String, Object> metadata) {
        this(content, toolCalls, invalidToolCalls, finishReason, metadata, ModelProtocolState.empty(), ModelUsage.empty());
    }

    public ModelResponse(String content, List<ModelToolCall> toolCalls, List<String> invalidToolCalls, String finishReason, Map<String, Object> metadata, ModelProtocolState protocolState) {
        this(content, toolCalls, invalidToolCalls, finishReason, metadata, protocolState, ModelUsage.empty());
    }
}
