package org.wrj.haifa.ai.deerflow.web;

import java.util.List;

public record ResearchPlanResponse(
        String planId,
        String threadId,
        String runId,
        String topic,
        List<String> researchQuestions,
        List<ResearchDimensionResponse> dimensions,
        List<String> searchQueries,
        String sourceCriteria,
        String expectedDeliverable,
        String status,
        String createdAt,
        String updatedAt
) {
}
