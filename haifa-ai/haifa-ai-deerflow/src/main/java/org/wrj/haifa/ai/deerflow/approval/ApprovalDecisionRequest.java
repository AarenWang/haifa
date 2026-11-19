package org.wrj.haifa.ai.deerflow.approval;

public record ApprovalDecisionRequest(
        ApprovalDecisionType decision,
        String comment
) {}
