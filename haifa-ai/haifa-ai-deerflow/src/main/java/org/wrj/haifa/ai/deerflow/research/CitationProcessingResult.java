package org.wrj.haifa.ai.deerflow.research;

import java.util.List;

public record CitationProcessingResult(
        String finalAnswer,
        List<CitationRecord> citations,
        List<ResearchSource> citedSources
) {

    public CitationProcessingResult {
        citations = citations == null ? List.of() : List.copyOf(citations);
        citedSources = citedSources == null ? List.of() : List.copyOf(citedSources);
        finalAnswer = finalAnswer == null ? "" : finalAnswer;
    }
}
