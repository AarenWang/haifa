package org.wrj.haifa.ai.deerflow.tool;

import java.util.Map;

public record ToolResult(String toolName, String content, Map<String, Object> metadata) {

    public static ToolResult of(String toolName, String content) {
        return new ToolResult(toolName, content, Map.of());
    }

    public static ToolResult of(String toolName, String content, Map<String, Object> metadata) {
        return new ToolResult(toolName, content, metadata == null ? Map.of() : metadata);
    }
}
