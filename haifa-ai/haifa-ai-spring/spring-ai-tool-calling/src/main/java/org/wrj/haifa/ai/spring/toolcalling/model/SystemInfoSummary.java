package org.wrj.haifa.ai.spring.toolcalling.model;

import java.util.Collections;
import java.util.List;

/**
 * Structured response returned by {@link org.wrj.haifa.ai.spring.toolcalling.system.SystemInfoService}.
 */
public record SystemInfoSummary(String title, String summary, List<String> highlights) implements ToolSummary {

    public SystemInfoSummary {
        highlights = highlights == null ? Collections.emptyList() : List.copyOf(highlights);
    }
}
