package org.wrj.haifa.ai.deerflow.persistence.store;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.memory.MemoryFactRecord;
import org.wrj.haifa.ai.deerflow.persistence.entity.MemoryFactEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.MemoryFactRepository;

@Component
public class MemoryFactStore {

    private final MemoryFactRepository repository;

    public MemoryFactStore(MemoryFactRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MemoryFactRecord save(MemoryFactRecord record) {
        MemoryFactEntity entity = toEntity(record);
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(UUID.randomUUID().toString());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        if (entity.getStatus() == null) {
            entity.setStatus("active");
        }

        MemoryFactEntity saved = repository.save(entity);
        return toRecord(saved);
    }

    @Transactional(readOnly = true)
    public List<MemoryFactRecord> findByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemoryFactRecord> findByUserIdAndStatus(String userId, String status) {
        return repository.findByUserIdAndStatus(userId, status).stream()
                .map(this::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MemoryFactRecord> findById(String id) {
        return repository.findById(id).map(this::toRecord);
    }

    @Transactional
    public void delete(String id) {
        repository.deleteById(id);
    }

    private MemoryFactEntity toEntity(MemoryFactRecord r) {
        MemoryFactEntity e = new MemoryFactEntity();
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
        e.setCreatedAt(r.createdAt());
        e.setUpdatedAt(r.updatedAt());
        e.setLastUsedAt(r.lastUsedAt());
        return e;
    }

    private MemoryFactRecord toRecord(MemoryFactEntity e) {
        return new MemoryFactRecord(
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
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getLastUsedAt()
        );
    }
}
