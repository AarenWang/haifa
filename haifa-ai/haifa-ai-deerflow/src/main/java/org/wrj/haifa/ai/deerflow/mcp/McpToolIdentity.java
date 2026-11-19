package org.wrj.haifa.ai.deerflow.mcp;

import io.modelcontextprotocol.spec.McpSchema;

public record McpToolIdentity(
        String exposedName,
        String connectionName,
        String originalToolName,
        long discoverySnapshotVersion,
        String description,
        String inputSchema,
        McpSchema.ToolAnnotations annotations,
        McpRiskClassification localRiskClassification,
        McpSemanticType semanticType,
        McpCapabilityOwner capabilityOwner) {
}
