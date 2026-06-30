package org.wrj.haifa.ai.deerflow.thread;

import java.time.Instant;
import java.util.Map;

public record MessageRecord(
        String messageId,
        String threadId,
        String runId,
        MessageRole role,
        String content,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
