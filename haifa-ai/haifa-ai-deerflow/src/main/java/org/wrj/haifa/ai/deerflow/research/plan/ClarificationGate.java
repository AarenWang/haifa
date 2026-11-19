package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.ResearchTimeWindow;
import org.wrj.haifa.ai.deerflow.agent.ResearchOutputFormat;

/**
 * Determines whether a research query needs clarification before proceeding.
 * Provides default values when specific aspects are unclear.
 */
@Component
public class ClarificationGate {

    /**
     * Check if the user request needs clarification.
     *
     * @param message        the user message
     * @param options        the research options provided
     * @return clarification result with default values if applicable
     */
    public ClarificationResult check(String message, ResearchOptions options) {
        if (!StringUtils.hasText(message)) {
            return ClarificationResult.needsClarification("Please provide a research topic or question.", "missing_info");
        }

        String lowerMsg = message.toLowerCase();
        List<String> gaps = new ArrayList<>();

        // 1. Check scope clarity
        if (isScopeUnclear(message)) {
            gaps.add("scope");
        }

        // 2. Check time window clarity
        if (options.timeWindow() == ResearchTimeWindow.LATEST && isTimeWindowAmbiguous(lowerMsg)) {
            gaps.add("timeWindow");
        }

        // 3. Check output format clarity
        if (options.outputFormat() == ResearchOutputFormat.ANSWER && isOutputFormatAmbiguous(lowerMsg)) {
            gaps.add("outputFormat");
        }

        // 4. Check for high-risk ambiguity
        if (hasHighRiskAmbiguity(lowerMsg)) {
            return ClarificationResult.needsClarification(
                    "Your request contains ambiguous terms that could significantly affect the research direction. Please clarify: " + extractAmbiguousTerms(message),
                    "ambiguous_requirement"
            );
        }

        if (gaps.isEmpty()) {
            return ClarificationResult.proceed(options);
        }

        // Apply defaults for gaps that can be safely inferred
        ResearchOptions effectiveOptions = options;
        StringBuilder note = new StringBuilder();
        for (String gap : gaps) {
            switch (gap) {
                case "timeWindow" -> note.append("Using default time window: ").append(options.timeWindow().name()).append(". ");
                case "outputFormat" -> note.append("Using default output format: ").append(options.outputFormat().name()).append(". ");
                case "scope" -> note.append("Research scope inferred from topic. ");
            }
        }
        return ClarificationResult.proceedWithDefaults(effectiveOptions, note.toString().trim());
    }

    private boolean isScopeUnclear(String message) {
        String[] vagueTerms = {"something", "anything", "whatever", "etc.", "...", "stuff", "things"};
        String lower = message.toLowerCase();
        for (String term : vagueTerms) {
            if (lower.contains(term)) return true;
        }
        return message.length() < 10; // Very short messages are likely unclear
    }

    private boolean isTimeWindowAmbiguous(String lowerMsg) {
        return !lowerMsg.contains("202") && !lowerMsg.contains("last year") && !lowerMsg.contains("recent")
                && !lowerMsg.contains("latest") && !lowerMsg.contains("2024") && !lowerMsg.contains("2025");
    }

    private boolean isOutputFormatAmbiguous(String lowerMsg) {
        return !lowerMsg.contains("report") && !lowerMsg.contains("summary") && !lowerMsg.contains("analysis")
                && !lowerMsg.contains("table") && !lowerMsg.contains("list");
    }

    private boolean hasHighRiskAmbiguity(String lowerMsg) {
        String[] ambiguousTerms = {"compare", "versus", "vs", "difference between", "best"};
        for (String term : ambiguousTerms) {
            if (lowerMsg.contains(term)) return true;
        }
        return false;
    }

    private String extractAmbiguousTerms(String message) {
        return message; // Simplified: return full message for context
    }

    /**
     * Result of a clarification check.
     */
    public record ClarificationResult(
            boolean needsClarification,
            String clarificationQuestion,
            String clarificationType,
            ResearchOptions effectiveOptions,
            boolean proceed,
            String defaultNote
    ) {
        public static ClarificationResult needsClarification(String question, String type) {
            return new ClarificationResult(true, question, type, null, false, "");
        }

        public static ClarificationResult proceed(ResearchOptions options) {
            return new ClarificationResult(false, "", "", options, true, "");
        }

        public static ClarificationResult proceedWithDefaults(ResearchOptions options, String note) {
            return new ClarificationResult(false, "", "", options, true, note);
        }
    }
}
