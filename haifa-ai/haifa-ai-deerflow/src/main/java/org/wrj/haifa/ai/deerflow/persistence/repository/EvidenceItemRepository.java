package org.wrj.haifa.ai.deerflow.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.EvidenceItemEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvidenceItemRepository extends JpaRepository<EvidenceItemEntity, String> {
    Optional<EvidenceItemEntity> findBySignature(String signature);
    List<EvidenceItemEntity> findByThreadId(String threadId);
    List<EvidenceItemEntity> findByRunId(String runId);
    List<EvidenceItemEntity> findBySourceId(String sourceId);
    List<EvidenceItemEntity> findByDimension(String dimension);
}
