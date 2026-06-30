package org.wrj.haifa.ai.deerflow.research.plan;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.ResearchDepth;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchPlannerTest {

    private final ResearchPlanner planner = new ResearchPlanner(null, null, null);

    @Test
    void emptyTopicReturnsFailure() {
        PlanGenerationResult result = planner.generatePlan("t1", "r1", "", ResearchOptions.defaults());
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("empty");
    }

    @Test
    void heuristicPlanForTechnologyTopic() {
        ResearchOptions opts = ResearchOptions.standard();
        PlanGenerationResult result = planner.generatePlan("t1", "r1", "cloud computing technology", opts);
        assertThat(result.success()).isTrue();
        assertThat(result.plan()).isNotNull();
        ResearchPlan plan = result.plan();
        assertThat(plan.topic()).isEqualTo("cloud computing technology");
        assertThat(plan.dimensions()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(plan.dimensionCount()).isGreaterThanOrEqualTo(3);
        assertThat(plan.searchQueries()).isNotEmpty();
        assertThat(plan.sourceCriteria()).isNotBlank();
        assertThat(plan.expectedDeliverable()).isNotBlank();
    }

    @Test
    void heuristicPlanForBusinessTopic() {
        ResearchOptions opts = ResearchOptions.standard();
        PlanGenerationResult result = planner.generatePlan("t1", "r1", "Tesla stock business analysis", opts);
        assertThat(result.success()).isTrue();
        ResearchPlan plan = result.plan();
        assertThat(plan.dimensions()).hasSizeGreaterThanOrEqualTo(3);
        boolean hasFinancial = plan.dimensions().stream()
                .anyMatch(d -> d.title().toLowerCase().contains("financial"));
        assertThat(hasFinancial).isTrue();
    }

    @Test
    void heuristicPlanForGenericTopic() {
        ResearchOptions opts = ResearchOptions.standard();
        PlanGenerationResult result = planner.generatePlan("t1", "r1", "history of pottery", opts);
        assertThat(result.success()).isTrue();
        ResearchPlan plan = result.plan();
        assertThat(plan.dimensions()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void quickDepthGeneratesFewerDimensions() {
        ResearchOptions quick = ResearchOptions.quick();
        PlanGenerationResult result = planner.generatePlan("t1", "r1", "quantum computing", quick);
        ResearchPlan plan = result.plan();
        assertThat(plan.dimensionCount()).isGreaterThanOrEqualTo(3).isLessThanOrEqualTo(4);
    }

    @Test
    void deepDepthGeneratesMoreDimensions() {
        ResearchOptions deep = ResearchOptions.deep();
        PlanGenerationResult result = planner.generatePlan("t1", "r1", "quantum computing", deep);
        ResearchPlan plan = result.plan();
        assertThat(plan.dimensionCount()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void allDimensionsStartAsPending() {
        PlanGenerationResult result = planner.generatePlan("t1", "r1", "topic", ResearchOptions.standard());
        ResearchPlan plan = result.plan();
        for (ResearchDimension dim : plan.dimensions()) {
            assertThat(dim.status()).isEqualTo(ResearchTaskStatus.PENDING);
        }
    }

    @Test
    void planContainsResearchQuestions() {
        PlanGenerationResult result = planner.generatePlan("t1", "r1", "topic", ResearchOptions.standard());
        ResearchPlan plan = result.plan();
        assertThat(plan.researchQuestions()).isNotEmpty();
    }
}
