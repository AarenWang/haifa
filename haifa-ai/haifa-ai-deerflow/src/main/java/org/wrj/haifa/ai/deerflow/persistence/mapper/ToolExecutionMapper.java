package org.wrj.haifa.ai.deerflow.persistence.mapper;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolExecutionEntity;

@Component
public class ToolExecutionMapper {

    private final JsonMapper jsonMapper;

    public ToolExecutionMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ToolExecutionEntity toEntity(String id, String runId, String threadId, String toolName, String description,
            ToolExecutionEntity.Status status, String inputJson, String output, String error, Long durationMs,
            Integer sequenceNo, java.util.Map<String, Object> metadata) {
        ToolExecutionEntity entity = new ToolExecutionEntity();
        entity.setId(id);
        entity.setRunId(runId);
        entity.setThreadId(threadId);
        entity.setToolName(toolName);
        entity.setDescription(description);
        entity.setStatus(status);
        entity.setInputJson(inputJson);
        entity.setOutput(output);
        entity.setError(error);
        entity.setDurationMs(durationMs);
        entity.setSequenceNo(sequenceNo);
        entity.setMetadataJson(jsonMapper.toJson(metadata));
        entity.setCreatedAt(java.time.Instant.now());
        return entity;
    }
}
