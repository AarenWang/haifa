package org.wrj.haifa.ai.deerflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;

/**
 * Structured message passed to or returned by the agent models.
 */
public record ModelMessage(
    Role role,
    String content,
    List<ModelToolCall> toolCalls,
    String toolCallId,
    String name,
    Map<String, Object> metadata,
    @JsonIgnore ModelProtocolState protocolState
) {
    public enum Role { SYSTEM, USER, ASSISTANT, TOOL }

    public ModelMessage(Role role, String content) {
        this(role, content, List.of(), null, null, Map.of(), ModelProtocolState.empty());
    }

    public ModelMessage(Role role, String content, Map<String, Object> metadata) {
        this(role, content, List.of(), null, null, metadata, ModelProtocolState.empty());
    }

    public ModelMessage(Role role, String content, List<ModelToolCall> toolCalls, String toolCallId, String name, Map<String, Object> metadata) {
        this(role, content, toolCalls, toolCallId, name, metadata, ModelProtocolState.empty());
    }

    public ModelMessage(Role role, String content, List<ModelToolCall> toolCalls, String toolCallId, String name, Map<String, Object> metadata, ModelProtocolState protocolState) {
        this.role = role;
        this.content = content == null ? "" : content;
        this.toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        this.toolCallId = toolCallId;
        this.name = name;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        this.protocolState = protocolState == null ? ModelProtocolState.empty() : protocolState;
    }
}
