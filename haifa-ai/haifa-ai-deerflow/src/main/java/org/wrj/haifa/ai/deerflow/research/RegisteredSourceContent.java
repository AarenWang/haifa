package org.wrj.haifa.ai.deerflow.research;

import org.wrj.haifa.ai.deerflow.webcontent.ExtractedWebContent;

public record RegisteredSourceContent(
        ResearchSource source,
        String rawContent,
        ExtractedWebContent extractedContent
) {

    public RegisteredSourceContent {
        rawContent = rawContent == null ? "" : rawContent;
    }
}
