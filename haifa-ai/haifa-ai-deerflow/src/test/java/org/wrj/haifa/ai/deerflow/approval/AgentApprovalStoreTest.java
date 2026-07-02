package org.wrj.haifa.ai.deerflow.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentApprovalStore;

class AgentApprovalStoreTest {

    private AgentApprovalStore store;
    private DeerFlowProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DeerFlowProperties();
        DeerFlowProperties.Approval approvalProps = new DeerFlowProperties.Approval();
        approvalProps.setDefaultTimeoutSeconds(120);
        properties.setApproval(approvalProps);
        store = new AgentApprovalStore(properties);
    }

    @Test
    void testCreateAndFind() {
        ApprovalCreateRequest request = new ApprovalCreateRequest(
                "run-1", "thread-1", "call-1", "run_script", "{\"script\":\"echo\"}",
                "hash-123", "risk-key", RiskLevel.MEDIUM, "reason", "", "preview", Map.of()
        );

        ApprovalRequestRecord record = store.create(request);
        assertThat(record.approvalId()).isNotBlank();
        assertThat(record.status()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(record.argsHash()).isEqualTo("hash-123");

        Optional<ApprovalRequestRecord> found = store.find(record.approvalId());
        assertThat(found).isPresent();
        assertThat(found.get().runId()).isEqualTo("run-1");
    }

    @Test
    void testFindPendingByThreadId() {
        ApprovalCreateRequest request = new ApprovalCreateRequest(
                "run-1", "thread-1", "call-1", "run_script", "{\"script\":\"echo\"}",
                "hash-123", "risk-key", RiskLevel.MEDIUM, "reason", "", "preview", Map.of()
        );

        store.create(request);
        Optional<ApprovalRequestRecord> pending = store.findPendingByThreadId("thread-1");
        assertThat(pending).isPresent();
        assertThat(pending.get().toolCallId()).isEqualTo("call-1");
    }

    @Test
    void testDecideApproveOnce() {
        ApprovalCreateRequest request = new ApprovalCreateRequest(
                "run-1", "thread-1", "call-1", "run_script", "{\"script\":\"echo\"}",
                "hash-123", "risk-key", RiskLevel.MEDIUM, "reason", "", "preview", Map.of()
        );

        ApprovalRequestRecord record = store.create(request);
        ApprovalDecisionRequest decisionReq = new ApprovalDecisionRequest(ApprovalDecisionType.APPROVE_ONCE, "ok comment");
        
        ApprovalRequestRecord updated = store.decide(record.approvalId(), decisionReq, "user");
        assertThat(updated.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(updated.decisionType()).isEqualTo(ApprovalDecisionType.APPROVE_ONCE);
        assertThat(updated.comment()).isEqualTo("ok comment");
    }

    @Test
    void testTimeoutExpiration() throws InterruptedException {
        DeerFlowProperties shortProps = new DeerFlowProperties();
        DeerFlowProperties.Approval approvalProps = new DeerFlowProperties.Approval();
        approvalProps.setDefaultTimeoutSeconds(1);
        shortProps.setApproval(approvalProps);
        
        AgentApprovalStore shortStore = new AgentApprovalStore(shortProps);
        ApprovalCreateRequest request = new ApprovalCreateRequest(
                "run-1", "thread-1", "call-1", "run_script", "{\"script\":\"echo\"}",
                "hash-123", "risk-key", RiskLevel.MEDIUM, "reason", "", "preview", Map.of()
        );

        ApprovalRequestRecord record = shortStore.create(request);
        assertThat(record.status()).isEqualTo(ApprovalStatus.PENDING);

        // Wait for 1.5 seconds
        Thread.sleep(1500);

        Optional<ApprovalRequestRecord> expired = shortStore.find(record.approvalId());
        assertThat(expired).isPresent();
        assertThat(expired.get().status()).isEqualTo(ApprovalStatus.EXPIRED);
    }
}
