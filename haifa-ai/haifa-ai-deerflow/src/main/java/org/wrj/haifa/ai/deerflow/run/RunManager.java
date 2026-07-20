package org.wrj.haifa.ai.deerflow.run;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.persistence.entity.RunEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.JsonMapper;
import org.wrj.haifa.ai.deerflow.persistence.mapper.RunMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.RunRepository;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;

@Component
public class RunManager {

    private static final Logger log = LoggerFactory.getLogger(RunManager.class);

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
                String reason = "Run cancelled because a newer run exists for this thread.";
                boolean cancelled = transition(entity.getRunId(), Set.of(RunStatus.RUNNING),
                        RunStatus.CANCELLED, reason);
                if (cancelled) {
                    entity.setStatus(RunStatus.CANCELLED);
                    entity.setError(reason);
                    entity.setUpdatedAt(Instant.now());
                }
                if (cancelled && this.agentLoopRunStore != null) {
                    this.agentLoopRunStore.markCancelled(entity.getRunId(), "SUPERSEDED_BY_NEWER_RUN");
                }
            }
            hasNewerRun = true;
        }
    }

    @Transactional
    public RunRecord markRunning(String runId) {
        transition(runId, Set.of(RunStatus.PENDING, RunStatus.SUSPENDED), RunStatus.RUNNING, null);
        return findCurrent(runId);
    }

    @Transactional
    public RunRecord markCompleted(String runId) {
        transition(runId, Set.of(RunStatus.RUNNING), RunStatus.COMPLETED, null);
        return findCurrent(runId);
    }

    @Transactional
    public RunRecord markSuspended(String runId) {
        transition(runId, Set.of(RunStatus.RUNNING), RunStatus.SUSPENDED, null);
        return findCurrent(runId);
    }

    @Transactional
    public RunRecord markCancelled(String runId) {
        transition(runId, Set.of(RunStatus.PENDING, RunStatus.RUNNING, RunStatus.SUSPENDED),
                RunStatus.CANCELLED, null);
        return findCurrent(runId);
    }

    @Transactional
    public RunRecord markFailed(String runId, String error) {
        transition(runId, Set.of(RunStatus.PENDING, RunStatus.RUNNING), RunStatus.FAILED, error);
        return findCurrent(runId);
    }

    @Transactional
    public boolean tryMarkCompleted(String runId) {
        return transition(runId, Set.of(RunStatus.RUNNING), RunStatus.COMPLETED, null);
    }

    @Transactional
    public boolean tryMarkCancelled(String runId) {
        return transition(runId, Set.of(RunStatus.PENDING, RunStatus.RUNNING, RunStatus.SUSPENDED),
                RunStatus.CANCELLED, null);
    }

    @Transactional
    public boolean tryMarkFailed(String runId, String error) {
        return transition(runId, Set.of(RunStatus.PENDING, RunStatus.RUNNING), RunStatus.FAILED, error);
    }

    private boolean transition(String runId, Set<RunStatus> allowed, RunStatus target, String error) {
        int updated = runRepository.transitionStatus(runId, allowed, target, error, Instant.now());
        if (updated == 1) {
            return true;
        }
        RunStatus current = runRepository.findByRunId(runId).map(RunEntity::getStatus).orElse(null);
        if (current == target) {
            return true;
        }
        log.warn("Run state transition rejected. runId={}, current={}, attempted={}, allowed={}",
                runId, current, target, allowed);
        return false;
    }

    private RunRecord findCurrent(String runId) {
        return runRepository.findByRunId(runId).map(runMapper::toRecord).orElse(null);
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
