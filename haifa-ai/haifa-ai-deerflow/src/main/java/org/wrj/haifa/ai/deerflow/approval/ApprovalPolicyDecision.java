package org.wrj.haifa.ai.deerflow.approval;

import java.util.Map;

public record ApprovalPolicyDecision(
        ApprovalPolicyDecisionType type,
        RiskLevel riskLevel,
        String reason,
        String riskKey,
        String preview,
        Map<String, Object> metadata
) {}
