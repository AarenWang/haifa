package org.wrj.haifa.ai.deerflow.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.Map;
import org.wrj.haifa.ai.deerflow.model.ModelProtocolState;

public record AgentEvent(
        String eventId,
        String runId,
        String threadId,
        AgentEventType type,
        String content,
        Map<String, Object> metadata,
        Instant createdAt,
        @JsonIgnore ModelProtocolState protocolState
) {

    public AgentEvent {
        metadata = metadata == null ? Map.of() : metadata;
        protocolState = protocolState == null ? ModelProtocolState.empty() : protocolState;
    }

    public AgentEvent(String eventId, String runId, String threadId, AgentEventType type,
            String content, Map<String, Object> metadata, Instant createdAt) {
        this(eventId, runId, threadId, type, content, metadata, createdAt, ModelProtocolState.empty());
    }

    public static AgentEvent of(String eventId, String runId, String threadId, AgentEventType type,
            String content, Map<String, Object> metadata) {
        return new AgentEvent(eventId, runId, threadId, type, content, metadata, Instant.now());
    }

    public static AgentEvent internal(String eventId, String runId, String threadId, AgentEventType type,
            String content, Map<String, Object> metadata, ModelProtocolState protocolState) {
        return new AgentEvent(eventId, runId, threadId, type, content, metadata, Instant.now(), protocolState);
    }
}
