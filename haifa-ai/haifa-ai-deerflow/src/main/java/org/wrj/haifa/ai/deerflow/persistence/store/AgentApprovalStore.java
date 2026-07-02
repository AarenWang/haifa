package org.wrj.haifa.ai.deerflow.persistence.store;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.approval.ApprovalCreateRequest;
import org.wrj.haifa.ai.deerflow.approval.ApprovalDecisionRequest;
import org.wrj.haifa.ai.deerflow.approval.ApprovalDecisionType;
import org.wrj.haifa.ai.deerflow.approval.ApprovalRequestRecord;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStatus;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStore;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class AgentApprovalStore implements ApprovalStore {

    private final Map<String, ApprovalRequestRecord> store = new ConcurrentHashMap<>();
    private final DeerFlowProperties properties;

    @Autowired
    public AgentApprovalStore(DeerFlowProperties properties) {
        this.properties = properties;
    }

    private ApprovalRequestRecord checkTimeout(ApprovalRequestRecord record) {
        if (record != null && record.status() == ApprovalStatus.PENDING && Instant.now().isAfter(record.expiresAt())) {
            ApprovalRequestRecord expired = new ApprovalRequestRecord(
                    record.approvalId(),
                    record.runId(),
                    record.threadId(),
                    record.toolCallId(),
                    record.toolName(),
                    record.argsJson(),
                    record.argsHash(),
                    record.riskKey(),
                    record.riskLevel(),
                    record.reason(),
                    record.purpose(),
                    record.preview(),
                    record.metadata(),
                    ApprovalStatus.EXPIRED,
                    record.createdAt(),
                    record.expiresAt(),
                    "system",
                    Instant.now(),
                    null,
                    "Approval expired due to timeout"
            );
            store.put(record.approvalId(), expired);
            return expired;
        }
        return record;
    }

    @Override
    public ApprovalRequestRecord create(ApprovalCreateRequest request) {
        findPendingByThreadId(request.threadId()).ifPresent(c -> markCancelled(c.approvalId()));

        String approvalId = UUID.randomUUID().toString();
        int timeoutSeconds = 120;
        if (properties.getApproval() != null) {
            timeoutSeconds = properties.getApproval().getDefaultTimeoutSeconds();
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(timeoutSeconds);

        ApprovalRequestRecord record = new ApprovalRequestRecord(
                approvalId,
                request.runId(),
                request.threadId(),
                request.toolCallId(),
                request.toolName(),
                request.argsJson(),
                request.argsHash(),
                request.riskKey(),
                request.riskLevel(),
                request.reason(),
                request.purpose(),
                request.preview(),
                request.metadata(),
                ApprovalStatus.PENDING,
                now,
                expiresAt,
                null,
                null,
                null,
                null
        );
        store.put(approvalId, record);
        return record;
    }

    @Override
    public Optional<ApprovalRequestRecord> find(String approvalId) {
        if (approvalId == null) {
            return Optional.empty();
        }
        ApprovalRequestRecord record = store.get(approvalId);
        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(checkTimeout(record));
    }

    @Override
    public Optional<ApprovalRequestRecord> findPendingByRunId(String runId) {
        if (runId == null) {
            return Optional.empty();
        }
        return store.values().stream()
                .map(this::checkTimeout)
                .filter(c -> runId.equals(c.runId()) && c.status() == ApprovalStatus.PENDING)
                .findFirst();
    }

    @Override
    public Optional<ApprovalRequestRecord> findPendingByThreadId(String threadId) {
        if (threadId == null) {
            return Optional.empty();
        }
        return store.values().stream()
                .map(this::checkTimeout)
                .filter(c -> threadId.equals(c.threadId()) && c.status() == ApprovalStatus.PENDING)
                .findFirst();
    }

    @Override
    public List<ApprovalRequestRecord> findByRunId(String runId) {
        if (runId == null) {
            return List.of();
        }
        return store.values().stream()
                .map(this::checkTimeout)
                .filter(c -> runId.equals(c.runId()))
                .toList();
    }

    @Override
    public List<ApprovalRequestRecord> findByThreadId(String threadId) {
        if (threadId == null) {
            return List.of();
        }
        return store.values().stream()
                .map(this::checkTimeout)
                .filter(c -> threadId.equals(c.threadId()))
                .toList();
    }

    @Override
    public ApprovalRequestRecord decide(String approvalId, ApprovalDecisionRequest decision, String resolvedBy) {
        ApprovalRequestRecord old = store.get(approvalId);
        if (old == null) {
            throw new IllegalArgumentException("Approval request not found: " + approvalId);
        }
        old = checkTimeout(old);
        if (old.status() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval request " + approvalId + " is not in PENDING status: " + old.status());
        }

        ApprovalStatus nextStatus = decision.decision() == ApprovalDecisionType.DENY ? ApprovalStatus.DENIED : ApprovalStatus.APPROVED;
        ApprovalRequestRecord updated = new ApprovalRequestRecord(
                old.approvalId(),
                old.runId(),
                old.threadId(),
                old.toolCallId(),
                old.toolName(),
                old.argsJson(),
                old.argsHash(),
                old.riskKey(),
                old.riskLevel(),
                old.reason(),
                old.purpose(),
                old.preview(),
                old.metadata(),
                nextStatus,
                old.createdAt(),
                old.expiresAt(),
                resolvedBy,
                Instant.now(),
                decision.decision(),
                decision.comment()
        );
        store.put(approvalId, updated);
        return updated;
    }

    @Override
    public ApprovalRequestRecord markExpired(String approvalId) {
        ApprovalRequestRecord old = store.get(approvalId);
        if (old != null) {
            ApprovalRequestRecord updated = new ApprovalRequestRecord(
                    old.approvalId(),
                    old.runId(),
                    old.threadId(),
                    old.toolCallId(),
                    old.toolName(),
                    old.argsJson(),
                    old.argsHash(),
                    old.riskKey(),
                    old.riskLevel(),
                    old.reason(),
                    old.purpose(),
                    old.preview(),
                    old.metadata(),
                    ApprovalStatus.EXPIRED,
                    old.createdAt(),
                    old.expiresAt(),
                    "system",
                    Instant.now(),
                    old.decisionType(),
                    old.comment()
            );
            store.put(approvalId, updated);
            return updated;
        }
        return null;
    }

    @Override
    public ApprovalRequestRecord markExecuted(String approvalId) {
        ApprovalRequestRecord old = store.get(approvalId);
        if (old != null) {
            ApprovalRequestRecord updated = new ApprovalRequestRecord(
                    old.approvalId(),
                    old.runId(),
                    old.threadId(),
                    old.toolCallId(),
                    old.toolName(),
                    old.argsJson(),
                    old.argsHash(),
                    old.riskKey(),
                    old.riskLevel(),
                    old.reason(),
                    old.purpose(),
                    old.preview(),
                    old.metadata(),
                    ApprovalStatus.EXECUTED,
                    old.createdAt(),
                    old.expiresAt(),
                    old.resolvedBy(),
                    old.resolvedAt(),
                    old.decisionType(),
                    old.comment()
            );
            store.put(approvalId, updated);
            return updated;
        }
        return null;
    }

    @Override
    public ApprovalRequestRecord markInvalidated(String approvalId, String reason) {
        ApprovalRequestRecord old = store.get(approvalId);
        if (old != null) {
            ApprovalRequestRecord updated = new ApprovalRequestRecord(
                    old.approvalId(),
                    old.runId(),
                    old.threadId(),
                    old.toolCallId(),
                    old.toolName(),
                    old.argsJson(),
                    old.argsHash(),
                    old.riskKey(),
                    old.riskLevel(),
                    old.reason(),
                    old.purpose(),
                    old.preview(),
                    old.metadata(),
                    ApprovalStatus.INVALIDATED,
                    old.createdAt(),
                    old.expiresAt(),
                    old.resolvedBy(),
                    old.resolvedAt(),
                    old.decisionType(),
                    reason
            );
            store.put(approvalId, updated);
            return updated;
        }
        return null;
    }

    @Override
    public void cancelByRunId(String runId) {
        if (runId == null) {
            return;
        }
        store.values().stream()
                .filter(c -> runId.equals(c.runId()) && c.status() == ApprovalStatus.PENDING)
                .forEach(c -> markCancelled(c.approvalId()));
    }

    private void markCancelled(String approvalId) {
        ApprovalRequestRecord old = store.get(approvalId);
        if (old != null) {
            ApprovalRequestRecord updated = new ApprovalRequestRecord(
                    old.approvalId(),
                    old.runId(),
                    old.threadId(),
                    old.toolCallId(),
                    old.toolName(),
                    old.argsJson(),
                    old.argsHash(),
                    old.riskKey(),
                    old.riskLevel(),
                    old.reason(),
                    old.purpose(),
                    old.preview(),
                    old.metadata(),
                    ApprovalStatus.CANCELLED,
                    old.createdAt(),
                    old.expiresAt(),
                    old.resolvedBy(),
                    old.resolvedAt(),
                    old.decisionType(),
                    old.comment()
            );
            store.put(approvalId, updated);
        }
    }

    public void clearAll() {
        store.clear();
    }
}
