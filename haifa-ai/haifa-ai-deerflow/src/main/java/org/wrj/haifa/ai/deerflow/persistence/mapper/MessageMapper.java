package org.wrj.haifa.ai.deerflow.persistence.mapper;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.persistence.entity.MessageEntity;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;

@Component
public class MessageMapper {

    private final JsonMapper jsonMapper;

    public MessageMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public MessageEntity toEntity(MessageRecord record) {
        MessageEntity entity = new MessageEntity();
        entity.setMessageId(record.messageId());
        entity.setThreadId(record.threadId());
        entity.setRunId(record.runId());
        entity.setRole(record.role());
        entity.setContent(record.content());
        entity.setMetadataJson(jsonMapper.toJson(record.metadata()));
        entity.setCreatedAt(record.createdAt());
        return entity;
    }

    public MessageRecord toRecord(MessageEntity entity) {
        return new MessageRecord(
                entity.getMessageId(),
                entity.getThreadId(),
                entity.getRunId(),
                entity.getRole(),
                entity.getContent(),
                jsonMapper.fromJson(entity.getMetadataJson()),
                entity.getCreatedAt()
        );
    }
}
