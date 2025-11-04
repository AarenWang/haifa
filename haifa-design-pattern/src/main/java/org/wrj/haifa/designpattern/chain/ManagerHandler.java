package org.wrj.haifa.designpattern.chain;

/**
 * 部门经理，审批额度适中，遇到超限将继续上送。
 */
public class ManagerHandler extends ApprovalHandler {

    private static final double APPROVAL_LIMIT = 10_000;

    @Override
    protected boolean canHandle(ApprovalRequest request) {
        ApprovalContext context = request.getContext();
        return context.getAmount() <= APPROVAL_LIMIT;
    }

    @Override
    protected void doHandle(ApprovalRequest request) {
        ApprovalContext context = request.getContext();
        // 简化逻辑：经理根据申请理由进行判断
        if (context.getReason().contains("违规")) {
            context.setDecision(Decision.REJECTED);
            context.setDecidedBy(getHandlerName());
            context.appendAudit("部门经理拒绝：申请理由不合规。");
        } else {
            context.setDecision(Decision.APPROVED);
            context.setDecidedBy(getHandlerName());
            context.appendAudit("部门经理审批通过，金额：" + context.getAmount());
        }
    }
}
