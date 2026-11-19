package org.wrj.haifa.ai.deerflow.approval;

import java.time.Instant;
import java.util.Map;

public record ApprovalRequestRecord(
        String approvalId,
        String runId,
        String threadId,
        String toolCallId,
        String toolName,
        String argsJson,
        String argsHash,
        String riskKey,
        RiskLevel riskLevel,
        String reason,
        String purpose,
        String preview,
        Map<String, Object> metadata,
        ApprovalStatus status,
        Instant createdAt,
        Instant expiresAt,
        String resolvedBy,
        Instant resolvedAt,
        ApprovalDecisionType decisionType,
        String comment
) {}
