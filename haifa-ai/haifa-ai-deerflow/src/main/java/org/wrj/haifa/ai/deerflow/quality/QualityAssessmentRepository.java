package org.wrj.haifa.ai.deerflow.quality;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityAssessmentRepository extends JpaRepository<QualityAssessment, String> {
    List<QualityAssessment> findByRunId(String runId);
}
