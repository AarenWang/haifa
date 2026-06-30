package org.wrj.haifa.ai.deerflow.persistence.mapper;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.loop.ModelStep;
import org.wrj.haifa.ai.deerflow.persistence.entity.ModelStepEntity;

@Component
public class ModelStepMapper {

    private final JsonMapper jsonMapper;

    public ModelStepMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ModelStepEntity toEntity(ModelStep step, String runId, String threadId) {
        ModelStepEntity entity = new ModelStepEntity();
        entity.setStepId(java.util.UUID.randomUUID().toString());
        entity.setRunId(runId);
        entity.setThreadId(threadId);
        entity.setStepIndex(step.stepIndex());
        entity.setStatus(ModelStepEntity.Status.COMPLETED);
        entity.setPromptSummary(step.prompt());
        entity.setResponseText(step.response());
        entity.setMetadataJson(jsonMapper.toJson(step.toolCalls() != null
                ? java.util.Map.of("toolCallCount", step.toolCalls().size())
                : java.util.Map.of()));
        entity.setCreatedAt(java.time.Instant.ofEpochMilli(step.startedAt()));
        entity.setUpdatedAt(java.time.Instant.ofEpochMilli(step.startedAt() + step.durationMs()));
        return entity;
    }
}
