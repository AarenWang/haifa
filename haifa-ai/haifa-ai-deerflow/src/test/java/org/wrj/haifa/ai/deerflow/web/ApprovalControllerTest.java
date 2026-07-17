package org.wrj.haifa.ai.deerflow.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.wrj.haifa.ai.deerflow.approval.ApprovalDecisionRequest;
import org.wrj.haifa.ai.deerflow.approval.ApprovalDecisionType;
import org.wrj.haifa.ai.deerflow.approval.ApprovalRequestRecord;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStatus;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStore;
import org.wrj.haifa.ai.deerflow.approval.RiskLevel;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

class ApprovalControllerTest {

    private ApprovalStore approvalStore;
    private DeerFlowProperties properties;
    private ApprovalController controller;

    @BeforeEach
    void setUp() {
        approvalStore = mock(ApprovalStore.class);
        properties = new DeerFlowProperties();
        controller = new ApprovalController(approvalStore, properties);
    }

    @Test
    void testGetPendingNotFound() {
        when(approvalStore.findPendingByThreadId("thread-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getPending("thread-1").block())
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private org.springframework.web.server.ServerWebExchange mockExchange(String userId) {
        org.springframework.web.server.ServerWebExchange exchange = mock(org.springframework.web.server.ServerWebExchange.class);
        org.springframework.http.server.reactive.ServerHttpRequest request = mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (userId != null) {
            headers.add("X-User-Id", userId);
        }
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        return exchange;
    }

    @Test
    void testApproveAlwaysForbiddenWhenDisabled() {
        DeerFlowProperties.Approval approvalProps = new DeerFlowProperties.Approval();
        approvalProps.setAllowAlwaysApproval(false); // ALWAYS is disabled!
        properties.setApproval(approvalProps);

        ApprovalDecisionRequest req = new ApprovalDecisionRequest(ApprovalDecisionType.APPROVE_ALWAYS, "comment");
        org.springframework.web.server.ServerWebExchange exchange = mockExchange("user");

        assertThatThrownBy(() -> controller.decide("app-1", req, exchange).block())
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void testApproveAlwaysAllowedWhenEnabled() {
        DeerFlowProperties.Approval approvalProps = new DeerFlowProperties.Approval();
        approvalProps.setAllowAlwaysApproval(true); // ALWAYS is enabled!
        properties.setApproval(approvalProps);

        ApprovalDecisionRequest req = new ApprovalDecisionRequest(ApprovalDecisionType.APPROVE_ALWAYS, "comment");
        ApprovalRequestRecord record = new ApprovalRequestRecord(
                "app-1", "run-1", "thread-1", "call-1", "run_script", "{}",
                "hash", "key", RiskLevel.MEDIUM, "reason", "comment", "preview",
                Map.of(), ApprovalStatus.APPROVED, java.time.Instant.now(), java.time.Instant.now().plusSeconds(120),
                "user", java.time.Instant.now(), ApprovalDecisionType.APPROVE_ALWAYS, "comment"
        );
        when(approvalStore.decide(eq("app-1"), eq(req), eq("user"))).thenReturn(record);
        org.springframework.web.server.ServerWebExchange exchange = mockExchange("user");

        ApprovalRequestRecord response = controller.decide("app-1", req, exchange).block();
        assertThat(response).isNotNull();
        assertThat(response.decisionType()).isEqualTo(ApprovalDecisionType.APPROVE_ALWAYS);
    }

    @Test
    void testDecisionUsesRequestUserIdForAudit() {
        ApprovalDecisionRequest req = new ApprovalDecisionRequest(ApprovalDecisionType.APPROVE_ONCE, "comment");
        ApprovalRequestRecord record = new ApprovalRequestRecord(
                "app-1", "run-1", "thread-1", "call-1", "run_script", "{}",
                "hash", "key", RiskLevel.MEDIUM, "reason", "comment", "preview",
                Map.of(), ApprovalStatus.APPROVED, java.time.Instant.now(), java.time.Instant.now().plusSeconds(120),
                "alice", java.time.Instant.now(), ApprovalDecisionType.APPROVE_ONCE, "comment"
        );
        when(approvalStore.decide(eq("app-1"), eq(req), eq("alice"))).thenReturn(record);
        org.springframework.web.server.ServerWebExchange exchange = mockExchange("alice");

        ApprovalRequestRecord response = controller.decide("app-1", req, exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.resolvedBy()).isEqualTo("alice");
    }

    @Test
    void protocolStateIsRemovedFromApprovalResponses() {
        ApprovalRequestRecord record = new ApprovalRequestRecord(
                "app-secret", "run-1", "thread-1", "call-1", "run_script", "{}",
                "hash", "key", RiskLevel.MEDIUM, "reason", "purpose", "preview",
                Map.of("protocolState", Map.of("adapter", "google-genai"), "safe", "value"),
                ApprovalStatus.PENDING, java.time.Instant.now(), java.time.Instant.now().plusSeconds(120),
                null, null, null, null);
        when(approvalStore.find("app-secret")).thenReturn(Optional.of(record));

        ApprovalRequestRecord response = controller.get("app-secret").block();

        assertThat(response).isNotNull();
        assertThat(response.metadata())
                .containsEntry("safe", "value")
                .doesNotContainKey("protocolState");
        assertThat(record.metadata()).containsKey("protocolState");
    }
}
