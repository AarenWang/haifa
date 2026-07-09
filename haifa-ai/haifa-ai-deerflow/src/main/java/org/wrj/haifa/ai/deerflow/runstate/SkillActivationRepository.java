package org.wrj.haifa.ai.deerflow.runstate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillActivationRepository extends JpaRepository<SkillActivation, String> {
    List<SkillActivation> findByRunId(String runId);
}
