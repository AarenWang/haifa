package org.wrj.haifa.ai.deerflow.persistence.store;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.memory.MemoryCandidateRecord;
import org.wrj.haifa.ai.deerflow.persistence.entity.MemoryCandidateEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.MemoryCandidateRepository;

@Component
public class MemoryCandidateStore {

    private final MemoryCandidateRepository repository;

    public MemoryCandidateStore(MemoryCandidateRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MemoryCandidateRecord save(MemoryCandidateRecord record) {
        MemoryCandidateEntity entity = toEntity(record);
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(UUID.randomUUID().toString());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        if (entity.getStatus() == null) {
            entity.setStatus("pending");
        }
        if (entity.getAction() == null || entity.getAction().isBlank()) {
            entity.setAction("ADD");
        }

        MemoryCandidateEntity saved = repository.save(entity);
        return toRecord(saved);
    }

    @Transactional(readOnly = true)
    public List<MemoryCandidateRecord> findByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemoryCandidateRecord> findByUserIdAndStatus(String userId, String status) {
        return repository.findByUserIdAndStatus(userId, status).stream()
                .map(this::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MemoryCandidateRecord> findById(String id) {
        return repository.findById(id).map(this::toRecord);
    }

    @Transactional
    public void delete(String id) {
        repository.deleteById(id);
    }

    private MemoryCandidateEntity toEntity(MemoryCandidateRecord r) {
        MemoryCandidateEntity e = new MemoryCandidateEntity();
        e.setId(r.id());
        e.setUserId(r.userId());
        e.setAgentId(r.agentId());
        e.setCategory(r.category());
        e.setContent(r.content());
        e.setSource(r.source());
        e.setSourceThreadId(r.sourceThreadId());
        e.setSourceRunId(r.sourceRunId());
        e.setConfidence(r.confidence());
        e.setStatus(r.status());
        e.setAction(r.action());
        e.setTargetFactId(r.targetFactId());
        e.setCreatedAt(r.createdAt());
        e.setUpdatedAt(r.updatedAt());
        return e;
    }

    private MemoryCandidateRecord toRecord(MemoryCandidateEntity e) {
        return new MemoryCandidateRecord(
                e.getId(),
                e.getUserId(),
                e.getAgentId(),
                e.getCategory(),
                e.getContent(),
                e.getSource(),
                e.getSourceThreadId(),
                e.getSourceRunId(),
                e.getConfidence(),
                e.getStatus(),
                e.getAction(),
                e.getTargetFactId(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
