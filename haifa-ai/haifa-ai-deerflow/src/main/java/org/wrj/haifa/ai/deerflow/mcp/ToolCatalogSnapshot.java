package org.wrj.haifa.ai.deerflow.mcp;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record ToolCatalogSnapshot(
        long version,
        Instant generatedAt,
        Map<String, McpToolIdentity> toolsByExposedName,
        Map<String, McpConnectionStatus> connectionStates) {

    public ToolCatalogSnapshot {
        toolsByExposedName = Map.copyOf(new LinkedHashMap<>(toolsByExposedName));
        connectionStates = Map.copyOf(new LinkedHashMap<>(connectionStates));
    }

    public static ToolCatalogSnapshot empty() {
        return new ToolCatalogSnapshot(0, Instant.EPOCH, Map.of(), Map.of());
    }
}
