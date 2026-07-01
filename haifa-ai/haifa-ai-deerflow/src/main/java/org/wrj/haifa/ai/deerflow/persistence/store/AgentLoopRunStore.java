package org.wrj.haifa.ai.deerflow.persistence.store;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentLoopRunEntity;
import org.wrj.haifa.ai.deerflow.persistence.mapper.AgentLoopRunMapper;
import org.wrj.haifa.ai.deerflow.persistence.repository.AgentLoopRunRepository;

@Component
public class AgentLoopRunStore {

    private final AgentLoopRunRepository agentLoopRunRepository;
    private final AgentLoopRunMapper agentLoopRunMapper;

    public AgentLoopRunStore(AgentLoopRunRepository agentLoopRunRepository, AgentLoopRunMapper agentLoopRunMapper) {
        this.agentLoopRunRepository = agentLoopRunRepository;
        this.agentLoopRunMapper = agentLoopRunMapper;
    }

    @Transactional
    public AgentLoopRunEntity create(String runId, String threadId, LoopConfig config) {
        String loopId = java.util.UUID.randomUUID().toString();
        AgentLoopRunEntity entity = agentLoopRunMapper.toEntity(loopId, runId, threadId, config);
        agentLoopRunRepository.save(entity);
        return entity;
    }

    @Transactional
    public void updateStepCount(String runId, int currentStep) {
        agentLoopRunRepository.findByRunId(runId).ifPresent(entity -> {
            entity.setCurrentStep(currentStep);
            entity.setUpdatedAt(Instant.now());
            agentLoopRunRepository.save(entity);
        });
    }

    @Transactional
    public void markCompleted(String runId, String stopReason) {
        agentLoopRunRepository.findByRunId(runId).ifPresent(entity -> {
            entity.setStatus(AgentLoopRunEntity.Status.COMPLETED);
            entity.setStopReason(stopReason);
            entity.setUpdatedAt(Instant.now());
            agentLoopRunRepository.save(entity);
        });
    }

    @Transactional
    public void markFailed(String runId, String stopReason) {
        agentLoopRunRepository.findByRunId(runId).ifPresent(entity -> {
            entity.setStatus(AgentLoopRunEntity.Status.FAILED);
            entity.setStopReason(stopReason);
            entity.setUpdatedAt(Instant.now());
            agentLoopRunRepository.save(entity);
        });
    }

    @Transactional
    public void markCancelled(String runId) {
        markCancelled(runId, "CANCELLED");
    }

    @Transactional
    public void markCancelled(String runId, String stopReason) {
        agentLoopRunRepository.findByRunId(runId).ifPresent(entity -> {
            entity.setStatus(AgentLoopRunEntity.Status.CANCELLED);
            entity.setStopReason(stopReason);
            entity.setUpdatedAt(Instant.now());
            agentLoopRunRepository.save(entity);
        });
    }

    @Transactional
    public void markTimeout(String runId) {
        agentLoopRunRepository.findByRunId(runId).ifPresent(entity -> {
            entity.setStatus(AgentLoopRunEntity.Status.TIMEOUT);
            entity.setStopReason("TIMEOUT");
            entity.setUpdatedAt(Instant.now());
            agentLoopRunRepository.save(entity);
        });
    }

    @Transactional(readOnly = true)
    public Optional<AgentLoopRunEntity> findByRunId(String runId) {
        return agentLoopRunRepository.findByRunId(runId);
    }
}
