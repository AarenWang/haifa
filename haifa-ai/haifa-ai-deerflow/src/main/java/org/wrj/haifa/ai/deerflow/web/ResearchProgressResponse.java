package org.wrj.haifa.ai.deerflow.web;

import java.util.List;

public record ResearchProgressResponse(
        int totalDimensions,
        int completedDimensions,
        int inProgressDimensions,
        int totalSources,
        int totalEvidence,
        String planStatus,
        double completionPercentage,
        List<String> gaps
) {
}
