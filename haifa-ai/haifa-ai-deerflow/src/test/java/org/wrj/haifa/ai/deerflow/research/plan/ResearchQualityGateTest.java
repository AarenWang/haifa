package org.wrj.haifa.ai.deerflow.research.plan;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchQualityGateTest {

    private final ResearchQualityGate gate = new ResearchQualityGate();

    private static ResearchPlan buildPlan(int dimensionCount, int completed) {
        List<ResearchDimension> dims = new ArrayList<>();
        for (int i = 0; i < dimensionCount; i++) {
            ResearchTaskStatus status = i < completed ? ResearchTaskStatus.COMPLETED : ResearchTaskStatus.PENDING;
            dims.add(new ResearchDimension(
                    "dim-" + i,
                    "Dimension " + i,
                    "desc",
                    status,
                    List.of("q" + i),
                    2, 0, 0, List.of()
            ));
        }
        return new ResearchPlan(
                "plan-1", "t1", "r1", "topic",
                List.of("Q1"), dims, List.of("q"),
                "criteria", "deliverable", "CREATED", null, null
        );
    }

    private static ResearchSource fetchedSource() {
        return new ResearchSource(
                "s1", "t1", "r1", "Title", "http://example.com",
                "http://example.com", "example.com", Instant.now(), Instant.now(),
                org.wrj.haifa.ai.deerflow.research.ResearchSourceType.WEB_PAGE, 0.8, "snippet", "hash"
        );
    }

    @Test
    void nullPlanFails() {
        QualityGateResult result = gate.evaluate(null, List.of(), List.of(), false);
        assertThat(result.passed()).isFalse();
        assertThat(result.gaps()).isNotEmpty();
    }

    @Test
    void insufficientDimensionsFail() {
        ResearchPlan plan = buildPlan(2, 2);
        List<ResearchSource> sources = List.of(fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource());
        QualityGateResult result = gate.evaluate(plan, sources, List.of(), false);
        assertThat(result.passed()).isFalse();
        assertThat(result.gaps().toString()).contains("dimensions");
    }

    @Test
    void insufficientSourcesFail() {
        ResearchPlan plan = buildPlan(4, 4);
        List<ResearchSource> sources = List.of(fetchedSource());
        QualityGateResult result = gate.evaluate(plan, sources, List.of(), false);
        assertThat(result.passed()).isFalse();
        assertThat(result.gaps().toString()).contains("sources");
    }

    @Test
    void perfectRunPasses() {
        ResearchPlan plan = buildPlan(4, 4);
        List<ResearchSource> sources = List.of(
                fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource()
        );
        List<EvidenceItem> evidence = List.of(
                new EvidenceItem("e1", "t1", "r1", "s1", "quote", "data statistics", "facts", 0.9, Instant.now()),
                new EvidenceItem("e2", "t1", "r1", "s1", "quote", "case study example", "cases", 0.8, Instant.now()),
                new EvidenceItem("e3", "t1", "r1", "s1", "quote", "expert opinion analyst", "opinions", 0.8, Instant.now()),
                new EvidenceItem("e4", "t1", "r1", "s1", "quote", "challenge risk limitation", "challenges", 0.7, Instant.now()),
                new EvidenceItem("e5", "t1", "r1", "s1", "quote", "criticism counter opposing", "counter", 0.7, Instant.now())
        );
        QualityGateResult result = gate.evaluate(plan, sources, evidence, true);
        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isGreaterThan(80.0);
    }

    @Test
    void missingEvidenceDiversityFails() {
        ResearchPlan plan = buildPlan(4, 4);
        List<ResearchSource> sources = List.of(
                fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource()
        );
        // Only one type of evidence
        List<EvidenceItem> evidence = List.of(
                new EvidenceItem("e1", "t1", "r1", "s1", "quote", "some data", "facts", 0.9, Instant.now())
        );
        QualityGateResult result = gate.evaluate(plan, sources, evidence, false);
        assertThat(result.passed()).isFalse();
        assertThat(result.gaps().toString()).contains("Missing");
    }

    @Test
    void uncompletedDimensionsFail() {
        ResearchPlan plan = buildPlan(5, 2);
        List<ResearchSource> sources = List.of(
                fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource()
        );
        QualityGateResult result = gate.evaluate(plan, sources, List.of(), false);
        assertThat(result.passed()).isFalse();
        assertThat(result.gaps().toString()).contains("completed");
    }

    @Test
    void citationRequirementWithoutEvidenceFails() {
        ResearchPlan plan = buildPlan(4, 4);
        List<ResearchSource> sources = List.of(
                fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource()
        );
        QualityGateResult result = gate.evaluate(plan, sources, List.of(), true);
        assertThat(result.passed()).isFalse();
        assertThat(result.citationComplete()).isFalse();
    }

    @Test
    void scoreReflectsCoverage() {
        ResearchPlan plan = buildPlan(3, 3);
        List<ResearchSource> sources = List.of(fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource(), fetchedSource());
        List<EvidenceItem> evidence = List.of(
                new EvidenceItem("e1", "t1", "r1", "s1", "quote", "statistics", "data", 0.9, Instant.now()),
                new EvidenceItem("e2", "t1", "r1", "s1", "quote", "case study", "cases", 0.8, Instant.now()),
                new EvidenceItem("e3", "t1", "r1", "s1", "quote", "expert", "opinions", 0.8, Instant.now()),
                new EvidenceItem("e4", "t1", "r1", "s1", "quote", "challenge", "limitations", 0.7, Instant.now()),
                new EvidenceItem("e5", "t1", "r1", "s1", "quote", "counter", "counter", 0.7, Instant.now())
        );
        QualityGateResult result = gate.evaluate(plan, sources, evidence, true);
        assertThat(result.score()).isGreaterThan(0.0).isLessThanOrEqualTo(100.0);
        assertThat(result.dimensionCount()).isEqualTo(3);
        assertThat(result.fetchedSourceCount()).isEqualTo(5);
    }
}
