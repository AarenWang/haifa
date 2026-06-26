package org.wrj.haifa.ai.deerflow.run;

import java.time.Instant;
import java.util.Map;

public record RunRecord(
        String runId,
        String threadId,
        String modelName,
        RunStatus status,
        String error,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {

    public RunRecord withStatus(RunStatus status) {
        return new RunRecord(runId, threadId, modelName, status, error, metadata, createdAt, Instant.now());
    }

    public RunRecord withError(String error) {
        return new RunRecord(runId, threadId, modelName, RunStatus.FAILED, error, metadata, createdAt, Instant.now());
    }
}
