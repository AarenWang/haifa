package org.wrj.haifa.ai.deerflow.mcp;

import java.time.Instant;

public record McpConnectionStatus(
        String connectionName,
        McpConnectionState state,
        boolean required,
        int toolCount,
        long snapshotVersion,
        Instant lastSuccessfulDiscovery,
        String errorType) {
}
