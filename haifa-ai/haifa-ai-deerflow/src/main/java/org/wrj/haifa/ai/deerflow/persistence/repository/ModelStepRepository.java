package org.wrj.haifa.ai.deerflow.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wrj.haifa.ai.deerflow.persistence.entity.ModelStepEntity;

@Repository
public interface ModelStepRepository extends JpaRepository<ModelStepEntity, String> {

    List<ModelStepEntity> findByRunIdOrderByStepIndexAsc(String runId);

    Optional<ModelStepEntity> findByRunIdAndStepIndex(String runId, Integer stepIndex);
}
