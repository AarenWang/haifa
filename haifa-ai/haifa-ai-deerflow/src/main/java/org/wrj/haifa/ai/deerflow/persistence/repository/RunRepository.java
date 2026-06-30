package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.RunEntity;

@Repository
public interface RunRepository extends JpaRepository<RunEntity, String> {

    Optional<RunEntity> findByRunId(String runId);

    List<RunEntity> findByThreadIdOrderByCreatedAtDesc(String threadId);
}
