package org.wrj.haifa.ai.deerflow.web;

import java.time.Instant;

public record ResearchSourceResponse(
        String sourceId,
        String title,
        String url,
        String domain,
        Instant publishedAt,
        Instant fetchedAt,
        String sourceType,
        double credibility,
        String snippet,
        String contentHash,
        boolean fetched,
        int citationCount
) {
}
