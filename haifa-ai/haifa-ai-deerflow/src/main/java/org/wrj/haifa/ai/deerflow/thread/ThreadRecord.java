package org.wrj.haifa.ai.deerflow.thread;

import java.time.Instant;
import java.util.Map;

public record ThreadRecord(
        String threadId,
        String title,
        ThreadStatus status,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {

    public ThreadRecord withTitle(String title) {
        return new ThreadRecord(threadId, title, status, metadata, createdAt, Instant.now());
    }

    public ThreadRecord withStatus(ThreadStatus status) {
        return new ThreadRecord(threadId, title, status, metadata, createdAt, Instant.now());
    }

    public ThreadRecord withMetadata(Map<String, Object> metadata) {
        return new ThreadRecord(threadId, title, status, metadata == null ? Map.of() : Map.copyOf(metadata),
                createdAt, Instant.now());
    }

    public ThreadRecord touched() {
        return new ThreadRecord(threadId, title, status, metadata, createdAt, Instant.now());
    }
}
