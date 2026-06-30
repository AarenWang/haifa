package org.wrj.haifa.ai.deerflow.web;

import java.time.Instant;

public record EvidenceItemResponse(
        String evidenceId,
        String sourceId,
        String sourceTitle,
        String sourceUrl,
        String quoteOrParaphrase,
        String claim,
        String dimension,
        double confidence,
        Instant extractedAt
) {
}
