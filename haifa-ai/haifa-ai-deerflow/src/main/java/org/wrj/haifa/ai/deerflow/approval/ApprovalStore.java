package org.wrj.haifa.ai.deerflow.approval;

import java.util.List;
import java.util.Optional;

public interface ApprovalStore {
    ApprovalRequestRecord create(ApprovalCreateRequest request);
    Optional<ApprovalRequestRecord> find(String approvalId);
    Optional<ApprovalRequestRecord> findPendingByRunId(String runId);
    Optional<ApprovalRequestRecord> findPendingByThreadId(String threadId);
    List<ApprovalRequestRecord> findByRunId(String runId);
    List<ApprovalRequestRecord> findByThreadId(String threadId);
    ApprovalRequestRecord decide(String approvalId, ApprovalDecisionRequest decision, String resolvedBy);
    ApprovalRequestRecord markExpired(String approvalId);
    ApprovalRequestRecord markExecuted(String approvalId);
    ApprovalRequestRecord markInvalidated(String approvalId, String reason);
    void cancelByRunId(String runId);
    List<ApprovalRequestRecord> findAlwaysApprovals();
}
