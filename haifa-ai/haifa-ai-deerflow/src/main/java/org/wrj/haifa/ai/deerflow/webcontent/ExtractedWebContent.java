package org.wrj.haifa.ai.deerflow.webcontent;

import java.time.Instant;
import java.util.List;

public record ExtractedWebContent(
        String title,
        String mainText,
        String author,
        Instant publishedAt,
        String canonicalUrl,
        List<String> outgoingLinks,
        List<String> reliableFields,
        List<String> bestEffortFields
) {

    public ExtractedWebContent {
        outgoingLinks = outgoingLinks == null ? List.of() : List.copyOf(outgoingLinks);
        reliableFields = reliableFields == null ? List.of() : List.copyOf(reliableFields);
        bestEffortFields = bestEffortFields == null ? List.of() : List.copyOf(bestEffortFields);
        title = title == null ? "" : title;
        mainText = mainText == null ? "" : mainText;
        author = author == null ? "" : author;
        canonicalUrl = canonicalUrl == null ? "" : canonicalUrl;
    }

    public boolean hasMainText() {
        return !mainText.isBlank();
    }
}
