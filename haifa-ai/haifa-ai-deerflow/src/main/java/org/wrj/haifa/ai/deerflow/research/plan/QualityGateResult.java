package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.List;

/**
 * Result of a quality gate evaluation.
 */
public record QualityGateResult(
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

    public QualityGateResult {
        gaps = gaps == null ? List.of() : List.copyOf(gaps);
        recommendation = recommendation == null ? "" : recommendation;
    }

    public static QualityGateResult passed(double score, int dimensionCount, int fetchedSourceCount) {
        return new QualityGateResult(true, score, List.of(), "Quality gate passed. Proceed to report generation.",
                dimensionCount, fetchedSourceCount, true, true, true, true, true, true, true);
    }

    public static QualityGateResult failed(double score, List<String> gaps, String recommendation,
                                           int dimensionCount, int fetchedSourceCount,
                                           boolean hasFacts, boolean hasData, boolean hasCases,
                                           boolean hasOpinions, boolean hasLimitations,
                                           boolean hasCounterView, boolean citationComplete) {
        return new QualityGateResult(false, score, gaps, recommendation,
                dimensionCount, fetchedSourceCount, hasFacts, hasData, hasCases,
                hasOpinions, hasLimitations, hasCounterView, citationComplete);
    }
}
