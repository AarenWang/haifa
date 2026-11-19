package org.wrj.haifa.ai.deerflow.mcp;

import java.time.Instant;
import java.util.Map;

public record McpToolExecutionContext(
        String runId,
        String threadId,
        McpToolIdentity identity,
        Map<String, Object> redactedArgumentSummary,
        Instant requestedAt) {
    public McpToolExecutionContext {
        redactedArgumentSummary = redactedArgumentSummary == null ? Map.of() : Map.copyOf(redactedArgumentSummary);
        requestedAt = requestedAt == null ? Instant.now() : requestedAt;
    }
}
