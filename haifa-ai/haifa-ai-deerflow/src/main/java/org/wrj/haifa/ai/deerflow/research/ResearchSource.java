package org.wrj.haifa.ai.deerflow.research;

import java.time.Instant;

public record ResearchSource(
        String sourceId,
        String threadId,
        String runId,
        String title,
        String url,
        String canonicalUrl,
        String domain,
        Instant publishedAt,
        Instant fetchedAt,
        ResearchSourceType sourceType,
        double credibility,
        String snippet,
        String contentHash
) {

    public ResearchSource {
        title = title == null ? "" : title;
        url = url == null ? "" : url;
        canonicalUrl = canonicalUrl == null ? "" : canonicalUrl;
        domain = domain == null ? "" : domain;
        snippet = snippet == null ? "" : snippet;
        contentHash = contentHash == null ? "" : contentHash;
        sourceType = sourceType == null ? ResearchSourceType.OTHER : sourceType;
    }

    public boolean fetched() {
        return fetchedAt != null;
    }

    public ResearchSource withFetchState(String title, String canonicalUrl, Instant publishedAt,
            Instant fetchedAt, ResearchSourceType sourceType, double credibility, String snippet, String contentHash) {
        return new ResearchSource(
                this.sourceId,
                this.threadId,
                this.runId,
                title == null || title.isBlank() ? this.title : title,
                this.url,
                canonicalUrl == null || canonicalUrl.isBlank() ? this.canonicalUrl : canonicalUrl,
                this.domain,
                publishedAt == null ? this.publishedAt : publishedAt,
                fetchedAt == null ? this.fetchedAt : fetchedAt,
                sourceType == null ? this.sourceType : sourceType,
                credibility,
                snippet == null || snippet.isBlank() ? this.snippet : snippet,
                contentHash == null ? this.contentHash : contentHash
        );
    }
}
