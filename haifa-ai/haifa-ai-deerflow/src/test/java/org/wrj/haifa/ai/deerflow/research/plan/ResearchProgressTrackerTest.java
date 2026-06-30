package org.wrj.haifa.ai.deerflow.research.plan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchProgressTrackerTest {

    private InMemoryResearchPlanStore planStore;
    private ResearchProgressTracker tracker;

    @BeforeEach
    void setUp() {
        planStore = new InMemoryResearchPlanStore();
        tracker = new ResearchProgressTracker(planStore);
    }

    private ResearchPlan createPlan(String runId) {
        List<ResearchDimension> dims = List.of(
                new ResearchDimension("d1", "Overview", "desc", ResearchTaskStatus.PENDING,
                        List.of("q1"), 2, 0, 0, List.of()),
                new ResearchDimension("d2", "Details", "desc", ResearchTaskStatus.PENDING,
                        List.of("q2"), 2, 0, 0, List.of()),
                new ResearchDimension("d3", "Challenges", "desc", ResearchTaskStatus.PENDING,
                        List.of("q3"), 2, 0, 0, List.of())
        );
        ResearchPlan plan = new ResearchPlan(
                "plan-1", "t1", runId, "topic",
                List.of("Q1"), dims, List.of("q1", "q2", "q3"),
                "criteria", "deliverable", "CREATED", null, null
        );
        planStore.save(plan);
        return plan;
    }

    @Test
    void markDimensionStartedUpdatesStatus() {
        String runId = "r1";
        createPlan(runId);
        tracker.markDimensionStarted(runId, "d1");

        Optional<ResearchPlan> updated = planStore.findByRunId(runId);
        assertThat(updated).isPresent();
        ResearchDimension d1 = updated.get().dimensions().stream().filter(d -> d.id().equals("d1")).findFirst().orElseThrow();
        assertThat(d1.status()).isEqualTo(ResearchTaskStatus.IN_PROGRESS);

        List<ResearchTask> tasks = planStore.findTasksByDimension(runId, "d1");
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).status()).isEqualTo(ResearchTaskStatus.IN_PROGRESS);
    }

    @Test
    void markDimensionCompletedUpdatesStatus() {
        String runId = "r1";
        createPlan(runId);
        tracker.markDimensionStarted(runId, "d1");
        tracker.markDimensionCompleted(runId, "d1");

        Optional<ResearchPlan> updated = planStore.findByRunId(runId);
        assertThat(updated).isPresent();
        ResearchDimension d1 = updated.get().dimensions().stream().filter(d -> d.id().equals("d1")).findFirst().orElseThrow();
        assertThat(d1.status()).isEqualTo(ResearchTaskStatus.COMPLETED);

        List<ResearchTask> tasks = planStore.findTasksByDimension(runId, "d1");
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).status()).isEqualTo(ResearchTaskStatus.COMPLETED);
    }

    @Test
    void updateDimensionProgressSetsCounts() {
        String runId = "r1";
        createPlan(runId);
        List<ResearchSource> sources = List.of(
                new ResearchSource("s1", "t1", runId, "Title", "http://example.com",
                        "http://example.com", "example.com", Instant.now(), Instant.now(),
                        org.wrj.haifa.ai.deerflow.research.ResearchSourceType.WEB_PAGE, 0.8, "snippet", "hash")
        );
        List<EvidenceItem> evidence = List.of(
                new EvidenceItem("e1", "t1", runId, "s1", "quote", "claim", "d1", 0.9, Instant.now())
        );
        tracker.updateDimensionProgress(runId, "d1", sources.size(), evidence);

        Optional<ResearchPlan> updated = planStore.findByRunId(runId);
        assertThat(updated).isPresent();
        ResearchDimension d1 = updated.get().dimensions().stream().filter(d -> d.id().equals("d1")).findFirst().orElseThrow();
        assertThat(d1.actualSourceCount()).isEqualTo(1);
        assertThat(d1.actualEvidenceCount()).isEqualTo(1);
        assertThat(d1.evidenceIds()).containsExactly("e1");
    }

    @Test
    void getProgressReturnsCorrectCounts() {
        String runId = "r1";
        createPlan(runId);
        tracker.markDimensionStarted(runId, "d1");
        tracker.markDimensionCompleted(runId, "d1");
        tracker.markDimensionStarted(runId, "d2");

        ResearchProgressTracker.ResearchProgress progress = tracker.getProgress(runId);
        assertThat(progress.totalDimensions()).isEqualTo(3);
        assertThat(progress.completedDimensions()).isEqualTo(1);
        assertThat(progress.inProgressDimensions()).isEqualTo(1);
        assertThat(progress.completionPercentage()).isEqualTo(100.0 / 3.0);
    }

    @Test
    void getProgressForUnknownRunReturnsEmpty() {
        ResearchProgressTracker.ResearchProgress progress = tracker.getProgress("unknown");
        assertThat(progress.totalDimensions()).isZero();
        assertThat(progress.completedDimensions()).isZero();
        assertThat(progress.planStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    void updateProgressWithNullLists() {
        String runId = "r1";
        createPlan(runId);
        tracker.updateDimensionProgress(runId, "d1", 0, null);

        Optional<ResearchPlan> updated = planStore.findByRunId(runId);
        assertThat(updated).isPresent();
        ResearchDimension d1 = updated.get().dimensions().stream().filter(d -> d.id().equals("d1")).findFirst().orElseThrow();
        assertThat(d1.actualSourceCount()).isZero();
        assertThat(d1.actualEvidenceCount()).isZero();
    }

    @Test
    void syncTasksFromPlanReflectsPlanEdits() {
        String runId = "r1";
        ResearchPlan plan = createPlan(runId);
        tracker.markDimensionStarted(runId, "d1");

        List<ResearchDimension> revisedDimensions = plan.dimensions().stream()
                .map(dimension -> dimension.id().equals("d2")
                        ? dimension.withStatus(ResearchTaskStatus.BLOCKED)
                        : dimension)
                .toList();
        planStore.save(plan.withDimensions(revisedDimensions));

        tracker.syncTasksFromPlan(runId);

        List<ResearchTask> d2Tasks = planStore.findTasksByDimension(runId, "d2");
        assertThat(d2Tasks).hasSize(1);
        assertThat(d2Tasks.get(0).status()).isEqualTo(ResearchTaskStatus.BLOCKED);
    }
}
