package org.wrj.haifa.ai.deerflow.memory;

import java.time.Instant;

public record MemoryFactRecord(
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
        Instant createdAt,
        Instant updatedAt,
        Instant lastUsedAt
) {
}
