package org.wrj.haifa.ai.deerflow.persistence.store;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.memory.AgentPersonaRecord;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentPersonaEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.AgentPersonaRepository;

@Component
public class AgentPersonaStore {

    private final AgentPersonaRepository repository;

    public AgentPersonaStore(AgentPersonaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AgentPersonaRecord save(AgentPersonaRecord record) {
        AgentPersonaEntity entity = toEntity(record);
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(UUID.randomUUID().toString());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());

        // If enabled is true, we must disable other personas for this user
        if (entity.isEnabled()) {
            List<AgentPersonaEntity> active = repository.findByUserIdAndEnabled(entity.getUserId(), true);
            for (AgentPersonaEntity p : active) {
                if (!p.getId().equals(entity.getId())) {
                    p.setEnabled(false);
                    p.setUpdatedAt(Instant.now());
                    repository.save(p);
                }
            }
        }

        AgentPersonaEntity saved = repository.save(entity);
        return toRecord(saved);
    }

    @Transactional(readOnly = true)
    public List<AgentPersonaRecord> findByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AgentPersonaRecord> findActiveByUserId(String userId) {
        return repository.findByUserIdAndEnabled(userId, true).stream()
                .map(this::toRecord)
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<AgentPersonaRecord> findById(String id) {
        return repository.findById(id).map(this::toRecord);
    }

    private AgentPersonaEntity toEntity(AgentPersonaRecord r) {
        AgentPersonaEntity e = new AgentPersonaEntity();
        e.setId(r.id());
        e.setUserId(r.userId());
        e.setAgentId(r.agentId());
        e.setName(r.name());
        e.setDescription(r.description());
        e.setSoul(r.soul());
        e.setEnabled(r.enabled());
        e.setCreatedAt(r.createdAt());
        e.setUpdatedAt(r.updatedAt());
        return e;
    }

    private AgentPersonaRecord toRecord(AgentPersonaEntity e) {
        return new AgentPersonaRecord(
                e.getId(),
                e.getUserId(),
                e.getAgentId(),
                e.getName(),
                e.getDescription(),
                e.getSoul(),
                e.isEnabled(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
