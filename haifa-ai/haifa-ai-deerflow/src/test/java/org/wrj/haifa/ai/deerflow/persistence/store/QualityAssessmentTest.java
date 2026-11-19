package org.wrj.haifa.ai.deerflow.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.quality.QualityAssessment;
import org.wrj.haifa.ai.deerflow.quality.QualityAssessmentStore;

@SpringBootTest
@ActiveProfiles("test")
class QualityAssessmentTest {

    @Autowired
    private QualityAssessmentStore qualityAssessmentStore;

    @Test
    void testQualityAssessment() {
        String runId = "run-quality-1";

        QualityAssessment qa = qualityAssessmentStore.save(
                runId,
                0.85,
                true,
                List.of("gap 1"),
                List.of("risk 1"),
                "synthesize",
                "Partial results due to search limits."
        );
        assertThat(qa.getAssessmentId()).isNotNull();
        assertThat(qa.getScore()).isEqualTo(0.85);
        assertThat(qa.getPassed()).isTrue();
        assertThat(qa.getGaps()).contains("gap 1");
        assertThat(qa.getRisks()).contains("risk 1");
        assertThat(qa.getNextAction()).isEqualTo("synthesize");
        assertThat(qa.getLimitations()).isEqualTo("Partial results due to search limits.");

        List<QualityAssessment> list = qualityAssessmentStore.findByRunId(runId);
        assertThat(list).hasSize(1);
    }
}
