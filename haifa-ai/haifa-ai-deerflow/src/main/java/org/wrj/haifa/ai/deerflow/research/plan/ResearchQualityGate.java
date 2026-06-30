package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;

/**
 * Rule-based quality gate for research runs.
 * Evaluates whether the research has sufficient coverage, sources, and diversity of evidence.
 */
@Component
public class ResearchQualityGate {

    private static final int MIN_DIMENSIONS = 3;
    private static final int MIN_SOURCES_STANDARD = 5;
    private static final int MIN_SOURCES_QUICK = 2;

    /**
     * Evaluate the quality of a research run based on its plan, sources, and evidence.
     *
     * @param plan           the research plan
     * @param sources        all sources collected during the run
     * @param evidenceItems  all evidence extracted during the run
     * @param requireCitations whether citations are required
     * @return quality gate result
     */
    public QualityGateResult evaluate(ResearchPlan plan, List<ResearchSource> sources, List<EvidenceItem> evidenceItems, boolean requireCitations) {
        List<String> gaps = new ArrayList<>();

        int dimensionCount = plan == null ? 0 : plan.dimensionCount();
        int completedDimensions = plan == null ? 0 : plan.completedDimensionCount();
        int fetchedSourceCount = (int) sources.stream().filter(ResearchSource::fetched).count();
        int expectedSources = plan == null ? 0 : plan.dimensions().stream().mapToInt(ResearchDimension::expectedSourceCount).sum();
        int minSources = expectedSources <= 0 ? MIN_SOURCES_STANDARD : Math.max(MIN_SOURCES_QUICK, Math.min(MIN_SOURCES_STANDARD, expectedSources));

        // 1. Check dimension coverage
        if (dimensionCount < MIN_DIMENSIONS) {
            gaps.add("Research plan covers fewer than " + MIN_DIMENSIONS + " dimensions (actual: " + dimensionCount + ")");
        }
        if (completedDimensions < dimensionCount) {
            gaps.add("Only " + completedDimensions + " of " + dimensionCount + " dimensions completed");
        }

        // 2. Check source count
        if (fetchedSourceCount < minSources) {
            gaps.add("Insufficient fetched sources: " + fetchedSourceCount + " (minimum: " + minSources + ")");
        }

        // 3. Check evidence diversity
        boolean hasFacts = false, hasData = false, hasCases = false, hasOpinions = false, hasLimitations = false, hasCounterView = false;
        boolean citationComplete = !requireCitations; // If not required, auto-pass

        for (EvidenceItem evidence : evidenceItems) {
            String dim = evidence.dimension() == null ? "" : evidence.dimension().toLowerCase();
            String claim = evidence.claim() == null ? "" : evidence.claim().toLowerCase();
            if (dim.contains("fact") || dim.contains("data")) hasFacts = true;
            if (dim.contains("case") || dim.contains("example")) hasCases = true;
            if (dim.contains("opinion") || dim.contains("expert")) hasOpinions = true;
            if (dim.contains("challenge") || dim.contains("limitation") || dim.contains("criticism") || dim.contains("counter")) {
                hasLimitations = true;
                hasCounterView = true;
            }
            if (claim.contains("fact") || claim.contains("reported") || claim.contains("according to")) hasFacts = true;
            // Also detect from claim content
            if (claim.contains("data") || claim.contains("statistic") || claim.contains("number")) hasData = true;
            if (claim.contains("case study") || claim.contains("example")) hasCases = true;
            if (claim.contains("expert") || claim.contains("analyst")) hasOpinions = true;
            if (claim.contains("challenge") || claim.contains("risk") || claim.contains("limitation")) hasLimitations = true;
            if (claim.contains("criticism") || claim.contains("counter") || claim.contains("opposing")) hasCounterView = true;
        }

        // Infer from dimensions if evidence is sparse
        if (plan != null) {
            for (ResearchDimension dim : plan.dimensions()) {
                String dTitle = dim.title().toLowerCase();
                if (dTitle.contains("fact") || dTitle.contains("data")) hasFacts = true;
                if (dTitle.contains("data") || dTitle.contains("statistics")) hasData = true;
                if (dTitle.contains("case") || dTitle.contains("example")) hasCases = true;
                if (dTitle.contains("opinion") || dTitle.contains("expert")) hasOpinions = true;
                if (dTitle.contains("challenge") || dTitle.contains("limitation") || dTitle.contains("criticism") || dTitle.contains("counter")) {
                    hasLimitations = true;
                    hasCounterView = true;
                }
            }
        }

        if (!hasFacts) gaps.add("Missing factual evidence dimension");
        if (!hasData) gaps.add("Missing data-driven evidence");
        if (!hasCases) gaps.add("Missing case studies or examples");
        if (!hasOpinions) gaps.add("Missing expert opinions or perspectives");
        if (!hasLimitations) gaps.add("Missing limitations or challenges coverage");
        if (!hasCounterView) gaps.add("Missing counter-arguments or opposing viewpoints");

        // 4. Citation completeness check
        if (requireCitations) {
            java.util.Set<String> fetchedSourceIds = sources.stream()
                    .filter(ResearchSource::fetched)
                    .map(ResearchSource::sourceId)
                    .collect(java.util.stream.Collectors.toSet());
            java.util.Set<String> citedSourceIds = evidenceItems.stream()
                    .map(EvidenceItem::sourceId)
                    .filter(sourceId -> sourceId != null && !sourceId.isBlank())
                    .collect(java.util.stream.Collectors.toSet());
            citationComplete = !fetchedSourceIds.isEmpty() && citedSourceIds.containsAll(fetchedSourceIds);
            if (!citationComplete) {
                gaps.add("No citations found despite citation requirement");
            }
        }

        // Calculate overall score
        ResearchCompletenessScore completenessScore = new ResearchCompletenessScore(
                calculateScore(dimensionCount, completedDimensions, fetchedSourceCount, minSources,
                        hasFacts, hasData, hasCases, hasOpinions, hasLimitations, hasCounterView, citationComplete),
                dimensionCount,
                completedDimensions,
                fetchedSourceCount,
                minSources,
                hasFacts,
                hasData,
                hasCases,
                hasOpinions,
                hasLimitations,
                hasCounterView,
                citationComplete,
                gaps
        );

        if (completenessScore.passed()) {
            return QualityGateResult.passed(completenessScore.overallScore(), dimensionCount, fetchedSourceCount);
        }

        String recommendation = buildRecommendation(fetchedSourceCount, minSources, completedDimensions, dimensionCount);
        return QualityGateResult.failed(completenessScore.overallScore(), completenessScore.gaps(), recommendation, dimensionCount, fetchedSourceCount,
                hasFacts, hasData, hasCases, hasOpinions, hasLimitations, hasCounterView, citationComplete);
    }

    private double calculateScore(int dimensionCount, int completedDimensions, int fetchedSourceCount, int minSources,
                                   boolean hasFacts, boolean hasData, boolean hasCases, boolean hasOpinions,
                                   boolean hasLimitations, boolean hasCounterView, boolean citationComplete) {
        double score = 0.0;
        score += Math.min(dimensionCount / 3.0, 1.0) * 20.0;
        score += Math.min(completedDimensions / Math.max(dimensionCount, 1.0), 1.0) * 15.0;
        score += Math.min(fetchedSourceCount / (double) minSources, 1.0) * 20.0;
        if (hasFacts) score += 5.0;
        if (hasData) score += 5.0;
        if (hasCases) score += 5.0;
        if (hasOpinions) score += 5.0;
        if (hasLimitations) score += 5.0;
        if (hasCounterView) score += 5.0;
        if (citationComplete) score += 15.0;
        return Math.min(score, 100.0);
    }

    private String buildRecommendation(int fetchedSourceCount, int minSources, int completedDimensions, int dimensionCount) {
        List<String> actions = new ArrayList<>();
        if (fetchedSourceCount < minSources) {
            actions.add("Continue searching and fetch at least " + (minSources - fetchedSourceCount) + " more full sources");
        }
        if (completedDimensions < dimensionCount) {
            actions.add("Complete remaining " + (dimensionCount - completedDimensions) + " dimensions");
        }
        if (actions.isEmpty()) {
            return "Research meets basic thresholds but has coverage gaps. Consider expanding scope.";
        }
        return String.join("; ", actions);
    }
}
