package org.wrj.haifa.ai.deerflow.research;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.webcontent.ExtractedWebContent;

@Component
public class EvidenceExtractor {

    public List<EvidenceItem> extract(String threadId, String runId, ResearchSource source, ExtractedWebContent content) {
        List<EvidenceItem> evidenceItems = new ArrayList<>();
        if (!StringUtils.hasText(content.mainText())) {
            return evidenceItems;
        }
        String[] sentences = content.mainText().split("(?<=[.!?])\\s+");
        for (String sentence : sentences) {
            String normalized = sentence.replaceAll("\\s+", " ").trim();
            if (normalized.length() < 50) {
                continue;
            }
            evidenceItems.add(new EvidenceItem(
                    UUID.randomUUID().toString(),
                    threadId,
                    runId,
                    source.sourceId(),
                    normalized,
                    normalized,
                    classifyDimension(normalized),
                    Math.max(0.35, source.credibility()),
                    Instant.now()
            ));
            if (evidenceItems.size() >= 3) {
                break;
            }
        }
        return evidenceItems;
    }

    private static String classifyDimension(String sentence) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\b\\d+(?:\\.\\d+)?%?.*")) {
            return "data";
        }
        if (lower.contains("because") || lower.contains("therefore") || lower.contains("suggests")) {
            return "analysis";
        }
        if (lower.contains("according to") || lower.contains("said") || lower.contains("reported")) {
            return "attribution";
        }
        return "overview";
    }
}
