package org.wrj.haifa.ai.deerflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.approval.ApprovalPolicyDecisionType;
import org.wrj.haifa.ai.deerflow.approval.ApprovalPolicyService;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStore;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.sandbox.CommandPolicy;

class McpApprovalIntegrationTest {

    @Test
    void fetchApprovalIsSnapshotBoundAndRedactsSecrets() {
        ApprovalStore store = mock(ApprovalStore.class);
        when(store.findByThreadId("thread-1")).thenReturn(List.of());
        DeerFlowProperties properties = new DeerFlowProperties();
        ApprovalPolicyService service = new ApprovalPolicyService(properties, store, mock(CommandPolicy.class));
        McpToolIdentity identity = new McpToolIdentity("mcp__fetch__fetch", "fetch", "fetch", 9,
                "fetch", "{}", null, McpRiskClassification.READ_ONLY, McpSemanticType.WEB_FETCH,
                McpCapabilityOwner.WEB_FETCH);
        String arguments = "{\"url\":\"https://example.com/a\",\"authorization\":\"Bearer secret\"}";

        var decision = service.evaluate(new ModelToolCall("call-1", identity.exposedName(), arguments),
                new McpAgentTool(identity, null),
                new AgentRunConfig("thread-1", "run-1", "test", false, false, 2, Path.of("."), Map.of()));

        assertThat(decision.type()).isEqualTo(ApprovalPolicyDecisionType.REQUIRE_APPROVAL);
        assertThat(decision.riskKey()).contains("connection=fetch", "snapshot=9", "host=example.com");
        assertThat(decision.preview()).contains("[REDACTED]").doesNotContain("Bearer secret");
        assertThat(decision.metadata()).containsEntry("semanticType", "WEB_FETCH");
    }
}
