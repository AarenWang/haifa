package org.wrj.haifa.ai.deerflow.tool.execution;

public record ToolBatchItemResult<T>(
        String callId,
        Status status,
        T value,
        Throwable error,
        boolean lateCompletion
) {
    public enum Status { SUCCESS, FAILED, CANCELLED }

    public static <T> ToolBatchItemResult<T> success(String callId, T value, boolean lateCompletion) {
        return new ToolBatchItemResult<>(callId, Status.SUCCESS, value, null, lateCompletion);
    }

    public static <T> ToolBatchItemResult<T> failed(String callId, Throwable error) {
        return new ToolBatchItemResult<>(callId, Status.FAILED, null, error, false);
    }

    public static <T> ToolBatchItemResult<T> cancelled(String callId) {
        return new ToolBatchItemResult<>(callId, Status.CANCELLED, null, null, false);
    }
}
