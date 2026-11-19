package org.wrj.haifa.ai.deerflow.agent;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ResearchOptions(
        ResearchDepth depth,
        ResearchTimeWindow timeWindow,
        @Min(1) @Max(50) Integer maxSources,
        Boolean requireCitations,
        ResearchOutputFormat outputFormat
) {

    public ResearchOptions {
        depth = depth == null ? ResearchDepth.STANDARD : depth;
        timeWindow = timeWindow == null ? ResearchTimeWindow.LATEST : timeWindow;
        maxSources = maxSources == null ? 10 : maxSources;
        requireCitations = requireCitations == null ? Boolean.TRUE : requireCitations;
        outputFormat = outputFormat == null ? ResearchOutputFormat.ANSWER : outputFormat;
    }

    public static ResearchOptions defaults() {
        return new ResearchOptions(null, null, null, null, null);
    }

    public static ResearchOptions quick() {
        return new ResearchOptions(ResearchDepth.QUICK, ResearchTimeWindow.LATEST, 5, Boolean.FALSE, ResearchOutputFormat.ANSWER);
    }

    public static ResearchOptions standard() {
        return new ResearchOptions(ResearchDepth.STANDARD, ResearchTimeWindow.LATEST, 10, Boolean.TRUE, ResearchOutputFormat.ANSWER);
    }

    public static ResearchOptions deep() {
        return new ResearchOptions(ResearchDepth.DEEP, ResearchTimeWindow.ALL_TIME, 20, Boolean.TRUE, ResearchOutputFormat.REPORT);
    }
}
