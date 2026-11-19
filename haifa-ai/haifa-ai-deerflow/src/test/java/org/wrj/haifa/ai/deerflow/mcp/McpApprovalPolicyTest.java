package org.wrj.haifa.ai.deerflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class McpApprovalPolicyTest {

    @Test
    void fetchAndUnknownAlwaysRequireApproval() {
        assertThat(McpApprovalPolicy.forTool(identity(McpRiskClassification.READ_ONLY, McpSemanticType.WEB_FETCH)).required()).isTrue();
        assertThat(McpApprovalPolicy.forTool(identity(McpRiskClassification.UNKNOWN, McpSemanticType.GENERIC)).required()).isTrue();
        assertThat(McpApprovalPolicy.forTool(identity(McpRiskClassification.READ_ONLY, McpSemanticType.EXTERNAL_READ)).required()).isFalse();
    }

    private static McpToolIdentity identity(McpRiskClassification risk, McpSemanticType semantic) {
        return new McpToolIdentity("mcp__test__read", "test", "read", 3, "read", "{}",
                null, risk, semantic,
                McpCapabilityOwner.WEB_FETCH);
    }
}
