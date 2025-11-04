package org.wrj.haifa.designpattern.chain;

/**
 * 团队主管，适用于审批小额申请。
 */
public class TeamLeadHandler extends ApprovalHandler {

    private static final double APPROVAL_LIMIT = 1_000;

    @Override
    protected boolean canHandle(ApprovalRequest request) {
        ApprovalContext context = request.getContext();
        return context.getAmount() <= APPROVAL_LIMIT;
    }

    @Override
    protected void doHandle(ApprovalRequest request) {
        ApprovalContext context = request.getContext();
        context.setDecision(Decision.APPROVED);
        context.setDecidedBy(getHandlerName());
        context.appendAudit("团队主管审批通过，金额：" + context.getAmount());
    }
}
