package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentEventEntity;

@Repository
public interface AgentEventRepository extends JpaRepository<AgentEventEntity, Long> {

    Optional<AgentEventEntity> findTopByOrderBySequenceNoDesc();

    List<AgentEventEntity> findByRunIdOrderBySequenceNoAsc(String runId);

    List<AgentEventEntity> findByThreadIdOrderByCreatedAtAsc(String threadId);
}
