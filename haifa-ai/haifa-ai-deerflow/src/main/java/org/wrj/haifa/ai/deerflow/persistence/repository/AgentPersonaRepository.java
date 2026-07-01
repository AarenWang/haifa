package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentPersonaEntity;

@Repository
public interface AgentPersonaRepository extends JpaRepository<AgentPersonaEntity, String> {

    List<AgentPersonaEntity> findByUserId(String userId);

    List<AgentPersonaEntity> findByUserIdAndEnabled(String userId, boolean enabled);
}
