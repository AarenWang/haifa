package org.wrj.haifa.ai.deerflow.run;

import java.time.Instant;
import java.util.Map;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public record RunRecord(
        String runId,
        String threadId,
        String modelName,
        @Enumerated(EnumType.STRING)
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

    public String mode() {
        if (metadata != null && metadata.get("mode") instanceof String m) {
            return m;
        }
        return "chat";
    }
}
