package org.wrj.haifa.ai.deerflow.research.plan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryResearchPlanStoreTest {

    @Test
    void savingUpdatedPlanDoesNotDuplicateThreadLookupEntries() {
        InMemoryResearchPlanStore store = new InMemoryResearchPlanStore();
        ResearchPlan plan = new ResearchPlan(
                "plan-1",
                "thread-1",
                "run-1",
                "topic",
                List.of("Q1"),
                List.of(new ResearchDimension(
                        "dim-1",
                        "Overview",
                        "desc",
                        ResearchTaskStatus.PENDING,
                        List.of("q1"),
                        2,
                        0,
                        0,
                        List.of())),
                List.of("q1"),
                "criteria",
                "deliverable",
                "CREATED",
                null,
                null
        );

        store.save(plan);
        store.save(plan.withStatus("IN_PROGRESS"));

        assertThat(store.findByThreadId("thread-1"))
                .hasSize(1)
                .first()
                .extracting(ResearchPlan::status)
                .isEqualTo("IN_PROGRESS");
    }
}
