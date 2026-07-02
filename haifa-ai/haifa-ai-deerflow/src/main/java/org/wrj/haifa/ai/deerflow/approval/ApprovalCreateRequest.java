package org.wrj.haifa.ai.deerflow.approval;

import java.util.Map;

public record ApprovalCreateRequest(
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
        Map<String, Object> metadata
) {}
