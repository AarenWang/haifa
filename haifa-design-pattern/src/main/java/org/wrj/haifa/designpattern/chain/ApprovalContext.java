package org.wrj.haifa.designpattern.chain;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批上下文，保存申请信息与处理结果。
 */
public class ApprovalContext {
    private final String applicant;
    private final String reason;
    private final double amount;
    private final List<String> auditTrail = new ArrayList<>();
    private Decision decision = Decision.PENDING;
    private String decidedBy;

    public ApprovalContext(String applicant, String reason, double amount) {
        this.applicant = applicant;
        this.reason = reason;
        this.amount = amount;
    }

    public String getApplicant() {
        return applicant;
    }

    public String getReason() {
        return reason;
    }

    public double getAmount() {
        return amount;
    }

    public void appendAudit(String logEntry) {
        auditTrail.add(logEntry);
    }

    public List<String> getAuditTrail() {
        return auditTrail;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }
}
