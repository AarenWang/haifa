package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.Map;

/**
 * Standardized tool call request within the agent loop.
 */
public record ToolCall(
        String id,
        String toolName,
        String arguments,
        Status status,
        Map<String, Object> metadata
) {

    public enum Status {
        PENDING,
        REQUESTED,
        EXECUTING,
        COMPLETED,
        FAILED
    }

    public ToolCall {
        id = id == null ? java.util.UUID.randomUUID().toString() : id;
        status = status == null ? Status.PENDING : status;
        metadata = metadata == null ? Map.of() : metadata;
    }

    public static ToolCall of(String toolName, String arguments) {
        return new ToolCall(null, toolName, arguments, Status.PENDING, Map.of());
    }

    public ToolCall withStatus(Status newStatus) {
        return new ToolCall(this.id, this.toolName, this.arguments, newStatus, this.metadata);
    }
}
