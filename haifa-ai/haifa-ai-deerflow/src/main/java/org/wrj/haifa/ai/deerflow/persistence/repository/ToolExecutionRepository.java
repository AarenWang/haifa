package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolExecutionEntity;

@Repository
public interface ToolExecutionRepository extends JpaRepository<ToolExecutionEntity, String> {

    List<ToolExecutionEntity> findByRunIdOrderBySequenceNoAsc(String runId);
}
