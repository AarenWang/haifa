package org.wrj.haifa.ai.spring.toolcalling.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Canonical representation returned by the tool and eventually surfaced in the
 * REST response.
 */
public record GeoKnowledgeSummary(String title, String summary, List<String> highlights) {

    public static GeoKnowledgeSummary empty() {
        return new GeoKnowledgeSummary("", "", Collections.emptyList());
    }

    public GeoKnowledgeSummary {
        highlights = highlights == null ? Collections.emptyList() : List.copyOf(highlights);
    }

    public String toBulletList() {
        if (highlights.isEmpty()) {
            return "No additional highlights were provided.";
        }
        return highlights.stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));
    }
}
