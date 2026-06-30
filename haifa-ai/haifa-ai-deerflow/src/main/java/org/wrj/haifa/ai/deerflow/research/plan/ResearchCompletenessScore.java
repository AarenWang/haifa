package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.List;

/**
 * Structured score representing how complete a research run is across multiple dimensions.
 */
public record ResearchCompletenessScore(
        double overallScore,
        int dimensionCount,
        int coveredDimensions,
        int fetchedSourceCount,
        int minRequiredSources,
        boolean hasFacts,
        boolean hasData,
        boolean hasCases,
        boolean hasOpinions,
        boolean hasLimitations,
        boolean hasCounterView,
        boolean citationComplete,
        List<String> gaps
) {

    public ResearchCompletenessScore {
        gaps = gaps == null ? List.of() : List.copyOf(gaps);
    }

    public boolean passed() {
        return coveredDimensions >= 3
                && fetchedSourceCount >= minRequiredSources
                && hasFacts
                && hasData
                && hasCases
                && hasOpinions
                && hasLimitations
                && hasCounterView
                && citationComplete;
    }

    public static ResearchCompletenessScore empty(int minRequiredSources) {
        return new ResearchCompletenessScore(
                0.0, 0, 0, 0, minRequiredSources,
                false, false, false, false, false, false, false,
                List.of("No research data available")
        );
    }
}
