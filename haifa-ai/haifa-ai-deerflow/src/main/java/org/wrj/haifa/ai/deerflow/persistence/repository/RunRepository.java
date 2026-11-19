package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.Collection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.RunEntity;
import org.wrj.haifa.ai.deerflow.run.RunStatus;

@Repository
public interface RunRepository extends JpaRepository<RunEntity, String> {

    Optional<RunEntity> findByRunId(String runId);

    List<RunEntity> findByThreadIdOrderByCreatedAtDesc(String threadId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RunEntity r set r.status = :target, r.error = :error, r.updatedAt = :updatedAt "
            + "where r.runId = :runId and r.status in :allowed")
    int transitionStatus(@Param("runId") String runId,
                         @Param("allowed") Collection<RunStatus> allowed,
                         @Param("target") RunStatus target,
                         @Param("error") String error,
                         @Param("updatedAt") Instant updatedAt);
}
