package org.wrj.haifa.ai.deerflow.persistence.mapper;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentEventEntity;

@Component
public class AgentEventMapper {

    private final JsonMapper jsonMapper;

    public AgentEventMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AgentEventEntity toEntity(AgentEvent event) {
        AgentEventEntity entity = new AgentEventEntity();
        entity.setEventId(event.eventId());
        entity.setRunId(event.runId());
        entity.setThreadId(event.threadId());
        entity.setType(event.type());
        entity.setContent(event.content());
        entity.setMetadataJson(jsonMapper.toJson(event.metadata()));
        entity.setCreatedAt(event.createdAt());
        return entity;
    }

    public AgentEvent toRecord(AgentEventEntity entity) {
        return new AgentEvent(
                entity.getEventId(),
                entity.getRunId(),
                entity.getThreadId(),
                entity.getType(),
                entity.getContent(),
                jsonMapper.fromJson(entity.getMetadataJson()),
                entity.getCreatedAt()
        );
    }
}
