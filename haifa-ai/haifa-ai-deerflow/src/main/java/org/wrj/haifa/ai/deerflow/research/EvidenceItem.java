package org.wrj.haifa.ai.deerflow.research;

import java.time.Instant;

public record EvidenceItem(
        String evidenceId,
        String threadId,
        String runId,
        String sourceId,
        String quoteOrParaphrase,
        String claim,
        String dimension,
        double confidence,
        Instant extractedAt
) {

    public EvidenceItem {
        quoteOrParaphrase = quoteOrParaphrase == null ? "" : quoteOrParaphrase;
        claim = claim == null ? "" : claim;
        dimension = dimension == null ? "general" : dimension;
    }
}
