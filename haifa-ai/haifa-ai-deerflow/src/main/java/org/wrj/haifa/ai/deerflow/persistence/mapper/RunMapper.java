package org.wrj.haifa.ai.deerflow.persistence.mapper;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.persistence.entity.RunEntity;
import org.wrj.haifa.ai.deerflow.run.RunRecord;

@Component
public class RunMapper {

    private final JsonMapper jsonMapper;

    public RunMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public RunEntity toEntity(RunRecord record) {
        RunEntity entity = new RunEntity();
        entity.setRunId(record.runId());
        entity.setThreadId(record.threadId());
        entity.setModelName(record.modelName());
        entity.setStatus(record.status());
        entity.setError(record.error());
        entity.setMetadataJson(jsonMapper.toJson(record.metadata()));
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        if (record.metadata() != null) {
            Object mode = record.metadata().get("mode");
            if (mode instanceof String s) {
                entity.setMode(s);
            }
        }
        return entity;
    }

    public RunRecord toRecord(RunEntity entity) {
        Map<String, Object> metadata = jsonMapper.fromJson(entity.getMetadataJson());
        if (entity.getMode() != null && !metadata.containsKey("mode")) {
            metadata = new java.util.HashMap<>(metadata);
            metadata.put("mode", entity.getMode());
        }
        return new RunRecord(
                entity.getRunId(),
                entity.getThreadId(),
                entity.getModelName(),
                entity.getStatus(),
                entity.getError(),
                metadata,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
