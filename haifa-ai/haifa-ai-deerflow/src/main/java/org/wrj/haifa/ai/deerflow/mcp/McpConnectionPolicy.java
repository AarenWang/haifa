package org.wrj.haifa.ai.deerflow.mcp;

import java.util.Set;

public record McpConnectionPolicy(
        String connectionName,
        boolean enabled,
        boolean required,
        Set<String> allowedTools,
        Set<String> deniedTools,
        McpStalePolicy stalePolicy) {
    public McpConnectionPolicy {
        allowedTools = allowedTools == null ? Set.of() : Set.copyOf(allowedTools);
        deniedTools = deniedTools == null ? Set.of() : Set.copyOf(deniedTools);
        stalePolicy = stalePolicy == null ? McpStalePolicy.DENY_NEW_CALLS : stalePolicy;
    }
}
