package org.wrj.haifa.ai.deerflow.persistence.mapper;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCall;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolCallEntity;

@Component
public class ToolCallMapper {

    private final JsonMapper jsonMapper;

    public ToolCallMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ToolCallEntity toEntity(ToolCall call, String runId, String threadId, Integer sequenceNo) {
        ToolCallEntity entity = new ToolCallEntity();
        entity.setToolCallId(call.id());
        entity.setRunId(runId);
        entity.setThreadId(threadId);
        entity.setToolName(call.toolName());
        entity.setArgumentsJson(call.arguments());
        entity.setStatus(mapStatus(call.status()));
        entity.setMetadataJson(jsonMapper.toJson(call.metadata()));
        entity.setCreatedAt(java.time.Instant.now());
        entity.setUpdatedAt(java.time.Instant.now());
        entity.setSequenceNo(sequenceNo);
        return entity;
    }

    public ToolCallEntity updateFromResult(ToolCallEntity entity, ToolCallResult result) {
        entity.setResult(result.result());
        entity.setError(result.error());
        entity.setDurationMs(result.durationMs());
        entity.setStatus(mapStatus(result.status()));
        entity.setMetadataJson(jsonMapper.toJson(result.metadata()));
        entity.setUpdatedAt(java.time.Instant.now());
        return entity;
    }

    private ToolCallEntity.Status mapStatus(ToolCall.Status status) {
        return switch (status) {
            case PENDING -> ToolCallEntity.Status.REQUESTED;
            case REQUESTED -> ToolCallEntity.Status.REQUESTED;
            case EXECUTING -> ToolCallEntity.Status.RUNNING;
            case COMPLETED -> ToolCallEntity.Status.COMPLETED;
            case FAILED -> ToolCallEntity.Status.FAILED;
        };
    }

    private ToolCallEntity.Status mapStatus(ToolCallResult.Status status) {
        return switch (status) {
            case PENDING -> ToolCallEntity.Status.REQUESTED;
            case SUCCESS -> ToolCallEntity.Status.COMPLETED;
            case FAILED -> ToolCallEntity.Status.FAILED;
            case TIMEOUT -> ToolCallEntity.Status.FAILED;
        };
    }
}
