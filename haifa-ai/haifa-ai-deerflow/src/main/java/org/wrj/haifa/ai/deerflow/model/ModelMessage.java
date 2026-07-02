package org.wrj.haifa.ai.deerflow.model;

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
    Map<String, Object> metadata
) {
    public enum Role { SYSTEM, USER, ASSISTANT, TOOL }

    public ModelMessage(Role role, String content) {
        this(role, content, List.of(), null, null, Map.of());
    }

    public ModelMessage(Role role, String content, Map<String, Object> metadata) {
        this(role, content, List.of(), null, null, metadata);
    }

    public ModelMessage(Role role, String content, List<ModelToolCall> toolCalls, String toolCallId, String name, Map<String, Object> metadata) {
        this.role = role;
        this.content = content == null ? "" : content;
        this.toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        this.toolCallId = toolCallId;
        this.name = name;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
