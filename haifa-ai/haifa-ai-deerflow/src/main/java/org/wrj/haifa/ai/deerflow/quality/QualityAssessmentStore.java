package org.wrj.haifa.ai.deerflow.quality;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class QualityAssessmentStore {

    private final QualityAssessmentRepository repository;

    public QualityAssessmentStore(QualityAssessmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public QualityAssessment save(String runId, Double score, Boolean passed, List<String> gaps, List<String> risks, String nextAction, String limitations) {
        QualityAssessment qa = new QualityAssessment();
        qa.setAssessmentId(UUID.randomUUID().toString());
        qa.setRunId(runId);
        qa.setScore(score != null ? score : 0.0);
        qa.setPassed(passed != null ? passed : false);
        qa.setGaps(gaps);
        qa.setRisks(risks);
        qa.setNextAction(nextAction != null ? nextAction : "continue");
        qa.setLimitations(limitations);
        qa.setCreatedAt(Instant.now());
        return repository.save(qa);
    }

    @Transactional(readOnly = true)
    public List<QualityAssessment> findByRunId(String runId) {
        return repository.findByRunId(runId);
    }
}
