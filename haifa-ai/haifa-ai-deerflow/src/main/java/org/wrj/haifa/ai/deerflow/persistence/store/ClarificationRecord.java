package org.wrj.haifa.ai.deerflow.persistence.store;

import java.time.Instant;

public record ClarificationRecord(
        String clarificationId,
        String threadId,
        String runId,
        String question,
        String clarificationType,
        String context,
        ClarificationStatus status,
        String answer,
        Instant createdAt,
        Instant answeredAt
) {}
