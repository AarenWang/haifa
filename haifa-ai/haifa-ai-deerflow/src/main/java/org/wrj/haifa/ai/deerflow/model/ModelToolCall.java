package org.wrj.haifa.ai.deerflow.model;

/**
 * Structured model tool call request.
 */
public record ModelToolCall(
    String id,
    String name,
    String arguments,
    String type
) {
    public ModelToolCall(String id, String name, String arguments) {
        this(id, name, arguments, "tool_call");
    }
}
