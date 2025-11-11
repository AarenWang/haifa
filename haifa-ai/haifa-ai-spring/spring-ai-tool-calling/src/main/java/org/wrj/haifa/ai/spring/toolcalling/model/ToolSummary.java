package org.wrj.haifa.ai.spring.toolcalling.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Common contract for tool responses that can be rendered by the local chat client.
 */
public interface ToolSummary {

    String title();

    String summary();

    List<String> highlights();

    default String toBulletList() {
        List<String> items = highlights();
        if (items == null || items.isEmpty()) {
            return "No additional highlights were provided.";
        }
        return items.stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));
    }

    static ToolSummary empty() {
        return new SimpleToolSummary("", "", Collections.emptyList());
    }

    static ToolSummary of(String title, String summary, List<String> highlights) {
        return new SimpleToolSummary(title, summary, highlights);
    }

    record SimpleToolSummary(String title, String summary, List<String> highlights) implements ToolSummary {

        public SimpleToolSummary {
            highlights = highlights == null ? Collections.emptyList() : List.copyOf(highlights);
        }
    }
}
