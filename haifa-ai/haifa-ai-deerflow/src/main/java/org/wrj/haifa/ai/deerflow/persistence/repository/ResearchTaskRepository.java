package org.wrj.haifa.ai.deerflow.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.ResearchTaskEntity;
import java.util.List;

@Repository
public interface ResearchTaskRepository extends JpaRepository<ResearchTaskEntity, String> {
    List<ResearchTaskEntity> findByRunId(String runId);
}
