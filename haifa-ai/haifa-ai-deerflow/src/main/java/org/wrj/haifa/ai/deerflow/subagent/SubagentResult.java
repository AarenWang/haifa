package org.wrj.haifa.ai.deerflow.subagent;

import java.util.List;
import java.util.Map;

/**
 * Structured result of a subagent execution.
 *
 * <p>Provides status, summary, evidence/source IDs, token usage, and error
 * so that the parent agent can synthesize multiple subagent results into a
 * unified conclusion.
 */
public record SubagentResult(
        String taskId,
        String parentRunId,
        String status, // STARTED, RUNNING, COMPLETED, FAILED, CANCELLED, TIMED_OUT
        String summary,
        List<String> evidenceIds,
        List<String> sourceIds,
        Map<String, Integer> tokenUsage,
        String error,
        long durationMs
) {

    public SubagentResult {
        taskId = taskId == null ? "" : taskId;
        parentRunId = parentRunId == null ? "" : parentRunId;
        status = status == null ? "FAILED" : status;
        summary = summary == null ? "" : summary;
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        sourceIds = sourceIds == null ? List.of() : List.copyOf(sourceIds);
        tokenUsage = tokenUsage == null ? Map.of() : Map.copyOf(tokenUsage);
        error = error == null ? "" : error;
        durationMs = Math.max(0, durationMs);
    }

    public boolean isSuccess() {
        return "COMPLETED".equals(status);
    }

    public boolean isTerminal() {
        return "COMPLETED".equals(status)
                || "FAILED".equals(status)
                || "CANCELLED".equals(status)
                || "TIMED_OUT".equals(status);
    }

    public static SubagentResult pending(String taskId, String parentRunId) {
        return new SubagentResult(taskId, parentRunId, "PENDING", "", List.of(), List.of(), Map.of(), "", 0);
    }

    public static SubagentResult success(String taskId, String parentRunId, String summary,
                                          List<String> evidenceIds, List<String> sourceIds,
                                          Map<String, Integer> tokenUsage, long durationMs) {
        return new SubagentResult(taskId, parentRunId, "COMPLETED", summary, evidenceIds, sourceIds, tokenUsage, "", durationMs);
    }

    public static SubagentResult failed(String taskId, String parentRunId, String error, long durationMs) {
        return new SubagentResult(taskId, parentRunId, "FAILED", "", List.of(), List.of(), Map.of(), error, durationMs);
    }

    public static SubagentResult cancelled(String taskId, String parentRunId, String error, long durationMs) {
        return new SubagentResult(taskId, parentRunId, "CANCELLED", "", List.of(), List.of(), Map.of(), error, durationMs);
    }

    public static SubagentResult timedOut(String taskId, String parentRunId, String error, long durationMs) {
        return new SubagentResult(taskId, parentRunId, "TIMED_OUT", "", List.of(), List.of(), Map.of(), error, durationMs);
    }
}
