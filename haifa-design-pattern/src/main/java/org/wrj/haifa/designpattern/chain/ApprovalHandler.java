package org.wrj.haifa.designpattern.chain;

/**
 * 职责链节点的通用契约。
 */
public abstract class ApprovalHandler {

    private ApprovalHandler next;

    public ApprovalHandler linkWith(ApprovalHandler nextHandler) {
        this.next = nextHandler;
        return nextHandler;
    }

    public final void handle(ApprovalRequest request) {
        if (canHandle(request)) {
            doHandle(request);
        } else if (next != null) {
            escalate(request);
            next.handle(request);
        } else {
            approvalFailed(request);
        }
    }

    protected abstract boolean canHandle(ApprovalRequest request);

    protected abstract void doHandle(ApprovalRequest request);

    protected void escalate(ApprovalRequest request) {
        request.getContext().appendAudit(getHandlerName() + " 无法审批，转上级继续处理。");
    }

    protected void approvalFailed(ApprovalRequest request) {
        ApprovalContext context = request.getContext();
        context.setDecision(Decision.REJECTED);
        context.setDecidedBy(getHandlerName());
        context.appendAudit("审批流程结束：无人能够处理，系统自动拒绝。");
    }

    protected String getHandlerName() {
        return getClass().getSimpleName();
    }
}
