package org.wrj.haifa.ai.deerflow.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentGraphCheckpointEntity;

import java.util.List;

@Repository
public interface AgentGraphCheckpointRepository extends JpaRepository<AgentGraphCheckpointEntity, String> {

    List<AgentGraphCheckpointEntity> findByRunIdOrderByCreatedAtAsc(String runId);

    List<AgentGraphCheckpointEntity> findByThreadIdOrderByCreatedAtAsc(String threadId);
}
