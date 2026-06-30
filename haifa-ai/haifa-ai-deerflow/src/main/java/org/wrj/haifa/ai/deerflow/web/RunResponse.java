package org.wrj.haifa.ai.deerflow.web;

import java.time.Instant;
import org.wrj.haifa.ai.deerflow.run.RunStatus;

public record RunResponse(
        String runId,
        String threadId,
        String modelName,
        RunStatus status,
        String error,
        String mode,
        Instant createdAt,
        Instant updatedAt
) {
}
