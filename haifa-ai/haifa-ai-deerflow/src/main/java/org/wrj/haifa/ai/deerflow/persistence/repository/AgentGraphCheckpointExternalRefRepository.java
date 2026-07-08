package org.wrj.haifa.ai.deerflow.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentGraphCheckpointExternalRefEntity;

@Repository
public interface AgentGraphCheckpointExternalRefRepository extends JpaRepository<AgentGraphCheckpointExternalRefEntity, String> {
}
