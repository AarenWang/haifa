package org.wrj.haifa.ai.deerflow.mcp;

import java.time.Instant;
import java.util.Map;

public record McpAuditEvent(
        Type type,
        String connectionName,
        String exposedToolName,
        long snapshotVersion,
        Instant occurredAt,
        Map<String, Object> safeMetadata) {
    public McpAuditEvent {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        safeMetadata = safeMetadata == null ? Map.of() : Map.copyOf(safeMetadata);
    }

    public enum Type {
        POLICY_DENIED, APPROVAL_DENIED, TRANSPORT_FAILED, AUTH_FAILED, PROTOCOL_FAILED,
        TOOL_FAILED, TOOL_SUCCEEDED, RESULT_REJECTED
    }
}
