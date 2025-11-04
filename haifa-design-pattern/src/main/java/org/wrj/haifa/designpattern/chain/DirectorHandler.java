package org.wrj.haifa.designpattern.chain;

/**
 * 总监 / VP，处理高金额审批，终结流程。
 */
public class DirectorHandler extends ApprovalHandler {

    @Override
    protected boolean canHandle(ApprovalRequest request) {
        // 总监可以处理所有金额
        return true;
    }

    @Override
    protected void doHandle(ApprovalRequest request) {
        ApprovalContext context = request.getContext();
        if (context.getReason().contains("战略") || context.getAmount() <= 100_000) {
            context.setDecision(Decision.APPROVED);
            context.setDecidedBy(getHandlerName());
            context.appendAudit("总监审批通过，批准金额：" + context.getAmount());
        } else {
            context.setDecision(Decision.REJECTED);
            context.setDecidedBy(getHandlerName());
            context.appendAudit("总监最终拒绝：预算超限且理由不足。");
        }
    }
}
