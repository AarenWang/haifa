package org.wrj.haifa.ai.deerflow.research.plan;

import java.time.Instant;
import java.util.List;

/**
 * Result of a research plan generation attempt.
 */
public record PlanGenerationResult(
        ResearchPlan plan,
        boolean needsClarification,
        String clarificationQuestion,
        String clarificationType,
        boolean success,
        String error
) {

    public PlanGenerationResult {
        clarificationQuestion = clarificationQuestion == null ? "" : clarificationQuestion;
        clarificationType = clarificationType == null ? "" : clarificationType;
        error = error == null ? "" : error;
    }

    public static PlanGenerationResult success(ResearchPlan plan) {
        return new PlanGenerationResult(plan, false, "", "", true, "");
    }

    public static PlanGenerationResult needsClarification(String question, String type) {
        return new PlanGenerationResult(null, true, question, type, false, "");
    }

    public static PlanGenerationResult failure(String error) {
        return new PlanGenerationResult(null, false, "", "", false, error);
    }
}
