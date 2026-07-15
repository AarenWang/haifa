package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.Map;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

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
        DENIED,
        NOT_FOUND,
        CANCELLED,
        TIMED_OUT,
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

    public static ToolCallResult from(ToolCall call, ToolResult result, long durationMs) {
        Status mapped = switch (result.status()) {
            case SUCCESS -> Status.SUCCESS;
            case DENIED -> Status.DENIED;
            case NOT_FOUND -> Status.NOT_FOUND;
            case CANCELLED -> Status.CANCELLED;
            case TIMED_OUT -> Status.TIMED_OUT;
            case FAILED -> Status.FAILED;
        };
        boolean success = mapped == Status.SUCCESS;
        return new ToolCallResult(call.id(), call.toolName(), call.arguments(), mapped,
                success ? result.content() : "", success ? "" : result.content(), durationMs, result.metadata());
    }
}
