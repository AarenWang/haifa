package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentLoopRunEntity;

@Repository
public interface AgentLoopRunRepository extends JpaRepository<AgentLoopRunEntity, String> {

    Optional<AgentLoopRunEntity> findByRunId(String runId);
}
