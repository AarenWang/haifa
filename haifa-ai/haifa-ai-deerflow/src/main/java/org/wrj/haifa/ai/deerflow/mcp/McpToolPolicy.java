package org.wrj.haifa.ai.deerflow.mcp;

public record McpToolPolicy(
        String connectionName,
        String originalToolName,
        boolean denied,
        McpRiskClassification risk,
        McpSemanticType semanticType,
        McpCapabilityOwner capabilityOwner) {
    public McpToolPolicy {
        risk = risk == null ? McpRiskClassification.UNKNOWN : risk;
        semanticType = semanticType == null ? McpSemanticType.GENERIC : semanticType;
    }
}
