package org.wrj.haifa.ai.deerflow.web;

import java.util.List;

public record QualityGateResponse(
        boolean passed,
        double score,
        List<String> gaps,
        String recommendation,
        int dimensionCount,
        int fetchedSourceCount,
        boolean hasFacts,
        boolean hasData,
        boolean hasCases,
        boolean hasOpinions,
        boolean hasLimitations,
        boolean hasCounterView,
        boolean citationComplete
) {
}
