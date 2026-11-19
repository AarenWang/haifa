package org.wrj.haifa.ai.deerflow.mcp;

public record McpApprovalPolicy(boolean required, String reason, String scopeKey) {
    public static McpApprovalPolicy forTool(McpToolIdentity identity) {
        boolean required = identity.semanticType() == McpSemanticType.WEB_FETCH
                || identity.localRiskClassification() != McpRiskClassification.READ_ONLY;
        String owner = identity.capabilityOwner() == null ? "NONE" : identity.capabilityOwner().name();
        return new McpApprovalPolicy(required,
                required ? "Local MCP risk policy requires approval" : "Trusted read-only MCP tool",
                identity.connectionName() + ":" + identity.originalToolName() + ":" + owner
                        + ":" + identity.discoverySnapshotVersion());
    }
}
