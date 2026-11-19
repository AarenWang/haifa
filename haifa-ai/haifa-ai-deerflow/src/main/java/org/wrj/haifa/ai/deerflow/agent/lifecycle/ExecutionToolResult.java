package org.wrj.haifa.ai.deerflow.agent.lifecycle;

import java.util.Map;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

public record ExecutionToolResult(String id, String toolName, String arguments, Status status, String result,
                                  String error, long durationMs, Map<String, Object> metadata) {
    public enum Status { PENDING, SUCCESS, FAILED, DENIED, NOT_FOUND, CANCELLED, TIMED_OUT, TIMEOUT }

    public ExecutionToolResult {
        status = status == null ? Status.PENDING : status;
        result = result == null ? "" : result;
        error = error == null ? "" : error;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static ExecutionToolResult from(ExecutionToolCall call, ToolResult result, long durationMs) {
        Status mapped = switch (result.status()) {
            case SUCCESS -> Status.SUCCESS;
            case DENIED -> Status.DENIED;
            case NOT_FOUND -> Status.NOT_FOUND;
            case CANCELLED -> Status.CANCELLED;
            case TIMED_OUT -> Status.TIMED_OUT;
            case FAILED -> Status.FAILED;
        };
        boolean success = mapped == Status.SUCCESS;
        return new ExecutionToolResult(call.id(), call.toolName(), call.arguments(), mapped,
                success ? result.content() : "", success ? "" : result.content(), durationMs, result.metadata());
    }
}
