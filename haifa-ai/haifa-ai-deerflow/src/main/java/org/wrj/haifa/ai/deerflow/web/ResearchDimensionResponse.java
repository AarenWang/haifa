package org.wrj.haifa.ai.deerflow.web;

import java.util.List;

public record ResearchDimensionResponse(
        String id,
        String title,
        String description,
        String status,
        List<String> searchQueries,
        int expectedSourceCount,
        int actualSourceCount,
        int actualEvidenceCount,
        List<String> evidenceIds
) {
}
