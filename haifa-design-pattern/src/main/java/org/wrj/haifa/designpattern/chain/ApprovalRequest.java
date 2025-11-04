package org.wrj.haifa.designpattern.chain;

/**
 * 聚合审批申请属性，方便 handler 使用。
 */
public class ApprovalRequest {
    private final ApprovalContext context;

    public ApprovalRequest(ApprovalContext context) {
        this.context = context;
    }

    public ApprovalContext getContext() {
        return context;
    }
}
