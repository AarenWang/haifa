package org.wrj.haifa.ai.deerflow.model;

import java.util.Map;

public record ToolCallProtocolState(
        int ordinal,
        String toolCallId,
        Map<String, Object> extensions
) {
    public ToolCallProtocolState {
        if (ordinal < 0) {
            throw new ModelProtocolStateException("Tool-call protocol state ordinal must not be negative");
        }
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}
