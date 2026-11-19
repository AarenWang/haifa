package org.wrj.haifa.ai.deerflow.persistence.mapper;

import java.util.HashMap;
import java.util.Map;
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

        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("toolCallCount", step.toolCalls() != null ? step.toolCalls().size() : 0);
        if (step.purpose() != null) {
            metadataMap.put("purpose", step.purpose().name());
        }
        if (step.eligibility() != null) {
            metadataMap.put("eligibility", step.eligibility().name());
        }
        if (step.promptFingerprint() != null) {
            metadataMap.put("promptFingerprint", step.promptFingerprint());
        }
        entity.setMetadataJson(jsonMapper.toJson(metadataMap));

        if (step.usage() != null) {
            entity.setTokenUsageJson(jsonMapper.toJsonObject(step.usage()));
        }


        entity.setCreatedAt(java.time.Instant.ofEpochMilli(step.startedAt()));
        entity.setUpdatedAt(java.time.Instant.ofEpochMilli(step.startedAt() + step.durationMs()));
        return entity;
    }
}
