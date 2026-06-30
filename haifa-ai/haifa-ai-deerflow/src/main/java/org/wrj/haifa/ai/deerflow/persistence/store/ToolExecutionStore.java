package org.wrj.haifa.ai.deerflow.persistence.store;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolExecutionEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.JsonMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.ToolExecutionRepository;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

@Component
public class ToolExecutionStore {

    private final ToolExecutionRepository toolExecutionRepository;
    private final JsonMapper jsonMapper;
    private final AtomicInteger sequenceCounter;

    public ToolExecutionStore(ToolExecutionRepository toolExecutionRepository, JsonMapper jsonMapper) {
        this.toolExecutionRepository = toolExecutionRepository;
        this.jsonMapper = jsonMapper;
        Integer maxSeq = toolExecutionRepository.findAll().stream()
                .map(ToolExecutionEntity::getSequenceNo)
                .filter(n -> n != null)
                .max(Integer::compare)
                .orElse(0);
        this.sequenceCounter = new AtomicInteger(maxSeq);
    }

    @Transactional
    public void saveStarted(String runId, String threadId, String toolName, String description, String inputJson,
            Map<String, Object> metadata) {
        ToolExecutionEntity entity = new ToolExecutionEntity();
        entity.setId(java.util.UUID.randomUUID().toString());
        entity.setRunId(runId);
        entity.setThreadId(threadId);
        entity.setToolName(toolName);
        entity.setDescription(description);
        entity.setStatus(ToolExecutionEntity.Status.STARTED);
        entity.setInputJson(inputJson);
        entity.setMetadataJson(metadataToJson(metadata));
        entity.setCreatedAt(Instant.now());
        entity.setSequenceNo(sequenceCounter.incrementAndGet());
        toolExecutionRepository.save(entity);
    }

    @Transactional
    public void saveCompleted(String runId, String threadId, String toolName, String output, Long durationMs,
            Map<String, Object> metadata) {
        ToolExecutionEntity entity = findLatestByRunIdAndToolName(runId, toolName);
        if (entity != null) {
            entity.setStatus(ToolExecutionEntity.Status.COMPLETED);
            entity.setOutput(output);
            entity.setDurationMs(durationMs);
            entity.setMetadataJson(mergeJson(entity.getMetadataJson(), metadata));
            toolExecutionRepository.save(entity);
        }
    }

    @Transactional
    public void saveFailed(String runId, String threadId, String toolName, String error, Long durationMs,
            Map<String, Object> metadata) {
        ToolExecutionEntity entity = findLatestByRunIdAndToolName(runId, toolName);
        if (entity != null) {
            entity.setStatus(ToolExecutionEntity.Status.FAILED);
            entity.setError(error);
            entity.setDurationMs(durationMs);
            entity.setMetadataJson(mergeJson(entity.getMetadataJson(), metadata));
            toolExecutionRepository.save(entity);
        }
    }

    @Transactional
    public void saveDenied(String runId, String threadId, String toolName, String reason, Map<String, Object> metadata) {
        ToolExecutionEntity entity = new ToolExecutionEntity();
        entity.setId(java.util.UUID.randomUUID().toString());
        entity.setRunId(runId);
        entity.setThreadId(threadId);
        entity.setToolName(toolName);
        entity.setStatus(ToolExecutionEntity.Status.DENIED);
        entity.setError(reason);
        entity.setMetadataJson(metadataToJson(metadata));
        entity.setCreatedAt(Instant.now());
        entity.setSequenceNo(sequenceCounter.incrementAndGet());
        toolExecutionRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ToolExecutionEntity> findByRunId(String runId) {
        return toolExecutionRepository.findByRunIdOrderBySequenceNoAsc(runId);
    }

    private ToolExecutionEntity findLatestByRunIdAndToolName(String runId, String toolName) {
        List<ToolExecutionEntity> list = toolExecutionRepository.findByRunIdOrderBySequenceNoAsc(runId);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (toolName.equals(list.get(i).getToolName())) {
                return list.get(i);
            }
        }
        return null;
    }

    private String metadataToJson(Map<String, Object> metadata) {
        String result = jsonMapper.toJson(metadata);
        return result != null ? result : "{}";
    }

    private String mergeJson(String existing, Map<String, Object> additional) {
        if (additional == null || additional.isEmpty()) {
            return existing;
        }
        Map<String, Object> existingMap = jsonMapper.fromJson(existing != null ? existing : "{}");
        if (existingMap.isEmpty()) {
            return metadataToJson(additional);
        }
        java.util.Map<String, Object> merged = new java.util.HashMap<>(existingMap);
        merged.putAll(additional);
        return metadataToJson(merged);
    }
}
