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
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;

@Component
public class RunManager {

    private final RunRepository runRepository;
    private final RunMapper runMapper;
    private final JsonMapper jsonMapper;
    private final AgentLoopRunStore agentLoopRunStore;

    public RunManager(RunRepository runRepository, RunMapper runMapper, JsonMapper jsonMapper,
            AgentLoopRunStore agentLoopRunStore) {
        this.runRepository = runRepository;
        this.runMapper = runMapper;
        this.jsonMapper = jsonMapper;
        this.agentLoopRunStore = agentLoopRunStore;
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

    @Transactional
    public List<RunRecord> listByThread(String threadId) {
        List<RunEntity> runs = runRepository.findByThreadIdOrderByCreatedAtDesc(threadId);
        cancelSupersededRunningRuns(runs);
        return runs.stream()
                .map(runMapper::toRecord)
                .toList();
    }

    private void cancelSupersededRunningRuns(List<RunEntity> runsNewestFirst) {
        boolean hasNewerRun = false;
        for (RunEntity entity : runsNewestFirst) {
            if (entity.getStatus() == RunStatus.RUNNING && hasNewerRun) {
                entity.setStatus(RunStatus.CANCELLED);
                entity.setError("Run cancelled because a newer run exists for this thread.");
                entity.setUpdatedAt(Instant.now());
                runRepository.save(entity);
                if (this.agentLoopRunStore != null) {
                    this.agentLoopRunStore.markCancelled(entity.getRunId(), "SUPERSEDED_BY_NEWER_RUN");
                }
            }
            hasNewerRun = true;
        }
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
    public RunRecord markSuspended(String runId) {
        return updateStatus(runId, RunStatus.SUSPENDED);
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
