package org.wrj.haifa.ai.deerflow.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.ResearchPlanEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResearchPlanRepository extends JpaRepository<ResearchPlanEntity, String> {
    Optional<ResearchPlanEntity> findByRunId(String runId);
    List<ResearchPlanEntity> findByThreadId(String threadId);
}
