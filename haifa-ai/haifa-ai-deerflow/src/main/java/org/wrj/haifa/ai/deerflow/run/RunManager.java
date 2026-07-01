package org.wrj.haifa.ai.deerflow.run;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.persistence.entity.RunEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.JsonMapper;
import org.wrj.haifa.ai.deerflow.persistence.mapper.RunMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.RunRepository;

@Component
public class RunManager {

    private final RunRepository runRepository;
    private final RunMapper runMapper;
    private final JsonMapper jsonMapper;

    public RunManager(RunRepository runRepository, RunMapper runMapper, JsonMapper jsonMapper) {
        this.runRepository = runRepository;
        this.runMapper = runMapper;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public RunRecord create(String threadId, String modelName, Map<String, Object> metadata) {
        String runId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        RunEntity entity = new RunEntity();
        entity.setRunId(runId);
        entity.setThreadId(threadId);
        entity.setModelName(modelName);
        entity.setStatus(RunStatus.PENDING);
        entity.setMetadataJson(metadataToJson(metadata));
        if (metadata != null && metadata.get("mode") instanceof String mode) {
            entity.setMode(mode);
        }
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        runRepository.save(entity);
        return runMapper.toRecord(entity);
    }

    @Transactional(readOnly = true)
    public Optional<RunRecord> find(String runId) {
        return runRepository.findByRunId(runId).map(runMapper::toRecord);
    }

    @Transactional(readOnly = true)
    public List<RunRecord> listByThread(String threadId) {
        return runRepository.findByThreadIdOrderByCreatedAtDesc(threadId).stream()
                .map(runMapper::toRecord)
                .toList();
    }

    @Transactional
    public RunRecord markRunning(String runId) {
        return updateStatus(runId, RunStatus.RUNNING);
    }

    @Transactional
    public RunRecord markCompleted(String runId) {
        return updateStatus(runId, RunStatus.COMPLETED);
    }

    @Transactional
    public RunRecord markCancelled(String runId) {
        return updateStatus(runId, RunStatus.CANCELLED);
    }

    @Transactional
    public RunRecord markFailed(String runId, String error) {
        return runRepository.findByRunId(runId).map(entity -> {
            entity.setStatus(RunStatus.FAILED);
            entity.setError(error);
            entity.setUpdatedAt(Instant.now());
            runRepository.save(entity);
            return runMapper.toRecord(entity);
        }).orElse(null);
    }

    private RunRecord updateStatus(String runId, RunStatus status) {
        return runRepository.findByRunId(runId).map(entity -> {
            entity.setStatus(status);
            entity.setUpdatedAt(Instant.now());
            runRepository.save(entity);
            return runMapper.toRecord(entity);
        }).orElse(null);
    }

    @Transactional(readOnly = true)
    public int count() {
        return (int) runRepository.count();
    }

    private String metadataToJson(Map<String, Object> metadata) {
        String result = jsonMapper.toJson(metadata);
        return result != null ? result : "{}";
    }
}
