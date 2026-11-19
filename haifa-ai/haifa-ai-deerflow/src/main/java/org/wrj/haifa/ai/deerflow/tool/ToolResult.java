package org.wrj.haifa.ai.deerflow.tool;

import java.util.Map;

public record ToolResult(String toolName, Status status, String content, Map<String, Object> metadata) {

    public enum Status {
        SUCCESS,
        FAILED,
        DENIED,
        NOT_FOUND,
        CANCELLED,
        TIMED_OUT
    }

    public ToolResult(String toolName, String content, Map<String, Object> metadata) {
        this(toolName, Status.SUCCESS, content, metadata);
    }

    public ToolResult {
        status = status == null ? Status.FAILED : status;
        content = content == null ? "" : content;
        metadata = metadata == null ? Map.of()
                : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(metadata));
    }

    public static ToolResult of(String toolName, String content) {
        return new ToolResult(toolName, content, Map.of());
    }

    public static ToolResult of(String toolName, String content, Map<String, Object> metadata) {
        return new ToolResult(toolName, content, metadata == null ? Map.of() : metadata);
    }

    public static ToolResult success(String toolName, String content, Map<String, Object> metadata) {
        return new ToolResult(toolName, Status.SUCCESS, content, metadata);
    }

    public static ToolResult failed(String toolName, String content) {
        return failed(toolName, content, Map.of());
    }

    public static ToolResult failed(String toolName, String content, Map<String, Object> metadata) {
        return new ToolResult(toolName, Status.FAILED, content, metadata);
    }

    public static ToolResult denied(String toolName, String content, Map<String, Object> metadata) {
        return new ToolResult(toolName, Status.DENIED, content, metadata);
    }

    public static ToolResult notFound(String toolName, String content) {
        return new ToolResult(toolName, Status.NOT_FOUND, content, Map.of("notFound", true));
    }

    public static ToolResult timedOut(String toolName, String content, Map<String, Object> metadata) {
        return new ToolResult(toolName, Status.TIMED_OUT, content, metadata);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
