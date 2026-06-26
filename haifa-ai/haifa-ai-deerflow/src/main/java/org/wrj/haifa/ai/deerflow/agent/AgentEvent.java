package org.wrj.haifa.ai.deerflow.agent;

import java.time.Instant;
import java.util.Map;

public record AgentEvent(
        String eventId,
        String runId,
        String threadId,
        AgentEventType type,
        String content,
        Map<String, Object> metadata,
        Instant createdAt
) {

    public static AgentEvent of(String eventId, String runId, String threadId, AgentEventType type,
            String content, Map<String, Object> metadata) {
        return new AgentEvent(eventId, runId, threadId, type, content, metadata == null ? Map.of() : metadata,
                Instant.now());
    }
}
