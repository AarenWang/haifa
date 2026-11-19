package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.ClarificationEntity;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStatus;

@Repository
public interface ClarificationRepository extends JpaRepository<ClarificationEntity, String> {

    Optional<ClarificationEntity> findByClarificationId(String clarificationId);

    Optional<ClarificationEntity> findFirstByThreadIdAndStatusOrderByCreatedAtDesc(
            String threadId, ClarificationStatus status);

    Optional<ClarificationEntity> findFirstByRunIdAndStatusOrderByCreatedAtDesc(
            String runId, ClarificationStatus status);

    Optional<ClarificationEntity> findFirstByRunIdOrderByCreatedAtDesc(String runId);

    List<ClarificationEntity> findByThreadIdAndStatus(String threadId, ClarificationStatus status);
}
