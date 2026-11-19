package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.ToolCallEntity;

@Repository
public interface ToolCallRepository extends JpaRepository<ToolCallEntity, String> {

    Optional<ToolCallEntity> findTopByOrderBySequenceNoDesc();

    List<ToolCallEntity> findByRunIdOrderBySequenceNoAsc(String runId);
}
