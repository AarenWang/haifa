package org.wrj.haifa.ai.deerflow.memory;

import java.time.Instant;

public record MemoryCandidateRecord(
        String id,
        String userId,
        String agentId,
        String category,
        String content,
        String source,
        String sourceThreadId,
        String sourceRunId,
        Double confidence,
        String status,
        String action,
        String targetFactId,
        String sourceError,
        Instant createdAt,
        Instant updatedAt
) {
}
