package org.wrj.haifa.ai.deerflow.persistence.mapper;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.persistence.entity.ThreadEntity;
import org.wrj.haifa.ai.deerflow.thread.ThreadRecord;

@Component
public class ThreadMapper {

    private final JsonMapper jsonMapper;

    public ThreadMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ThreadEntity toEntity(ThreadRecord record) {
        ThreadEntity entity = new ThreadEntity();
        entity.setThreadId(record.threadId());
        entity.setTitle(record.title());
        entity.setStatus(record.status());
        entity.setMetadataJson(jsonMapper.toJson(record.metadata()));
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }

    public ThreadRecord toRecord(ThreadEntity entity) {
        return new ThreadRecord(
                entity.getThreadId(),
                entity.getTitle(),
                entity.getStatus(),
                jsonMapper.fromJson(entity.getMetadataJson()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
