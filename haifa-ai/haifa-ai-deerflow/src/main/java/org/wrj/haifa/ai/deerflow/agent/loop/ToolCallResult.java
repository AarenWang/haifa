package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.Map;

/**
 * Standardized tool call result within the agent loop.
 */
public record ToolCallResult(
        String id,
        String toolName,
        String arguments,
        Status status,
        String result,
        String error,
        long durationMs,
        Map<String, Object> metadata
) {

    public enum Status {
        PENDING,
        SUCCESS,
        FAILED,
        TIMEOUT
    }

    public ToolCallResult {
        id = id == null ? "" : id;
        status = status == null ? Status.PENDING : status;
        result = result == null ? "" : result;
        error = error == null ? "" : error;
        metadata = metadata == null ? Map.of() : metadata;
    }

    public static ToolCallResult success(String id, String toolName, String arguments, String result, long durationMs) {
        return new ToolCallResult(id, toolName, arguments, Status.SUCCESS, result, "", durationMs, Map.of());
    }

    public static ToolCallResult failed(String id, String toolName, String arguments, String error, long durationMs) {
        return new ToolCallResult(id, toolName, arguments, Status.FAILED, "", error, durationMs, Map.of("failed", true));
    }

    public static ToolCallResult from(ToolCall call, String result, long durationMs) {
        return success(call.id(), call.toolName(), call.arguments(), result, durationMs);
    }

    public static ToolCallResult fromError(ToolCall call, String error, long durationMs) {
        return failed(call.id(), call.toolName(), call.arguments(), error, durationMs);
    }
}
