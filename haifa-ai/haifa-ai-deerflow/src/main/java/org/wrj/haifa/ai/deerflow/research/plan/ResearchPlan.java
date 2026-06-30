package org.wrj.haifa.ai.deerflow.research.plan;

import java.time.Instant;
import java.util.List;

/**
 * A structured research plan produced before the research loop begins.
 * It captures the topic, research questions, dimensions to explore,
 * search queries, source criteria, and expected deliverable.
 */
public record ResearchPlan(
        String planId,
        String threadId,
        String runId,
        String topic,
        List<String> researchQuestions,
        List<ResearchDimension> dimensions,
        List<String> searchQueries,
        String sourceCriteria,
        String expectedDeliverable,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public ResearchPlan {
        status = status == null ? "CREATED" : status;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
        researchQuestions = researchQuestions == null ? List.of() : List.copyOf(researchQuestions);
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        searchQueries = searchQueries == null ? List.of() : List.copyOf(searchQueries);
        sourceCriteria = sourceCriteria == null ? "" : sourceCriteria;
        expectedDeliverable = expectedDeliverable == null ? "" : expectedDeliverable;
    }

    public ResearchPlan withStatus(String newStatus) {
        return new ResearchPlan(planId, threadId, runId, topic, researchQuestions, dimensions,
                searchQueries, sourceCriteria, expectedDeliverable, newStatus, createdAt, Instant.now());
    }

    public ResearchPlan withDimensions(List<ResearchDimension> newDimensions) {
        return new ResearchPlan(planId, threadId, runId, topic, researchQuestions, newDimensions,
                searchQueries, sourceCriteria, expectedDeliverable, status, createdAt, Instant.now());
    }

    public int dimensionCount() {
        return dimensions.size();
    }

    public int completedDimensionCount() {
        return (int) dimensions.stream().filter(d -> d.status() == ResearchTaskStatus.COMPLETED).count();
    }
}
