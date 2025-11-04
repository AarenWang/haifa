package org.wrj.haifa.designpattern.chain;

import java.util.List;

/**
 * 演示公司内部审批流程的职责链用法。
 */
public class ChainDemo {

    public static void main(String[] args) {
        ApprovalHandler teamLead = new TeamLeadHandler();
        ApprovalHandler manager = new ManagerHandler();
        ApprovalHandler director = new DirectorHandler();
        teamLead.linkWith(manager).linkWith(director);

        List<ApprovalContext> requests = List.of(
                new ApprovalContext("Alice", "营销活动请款", 500),
                new ApprovalContext("Bob", "违规报销", 3_000),
                new ApprovalContext("Charlie", "部门设备采购", 8_000),
                new ApprovalContext("Diana", "战略项目投资", 120_000)
        );

        for (ApprovalContext context : requests) {
            System.out.println("\n开始处理申请: " + context.getApplicant() + " -> " + context.getReason() + " (" + context.getAmount() + ")");
            teamLead.handle(new ApprovalRequest(context));
            System.out.println("最终状态: " + context.getDecision() + ", 审批人: " + context.getDecidedBy());
            context.getAuditTrail().forEach(log -> System.out.println(" - " + log));
        }
    }
}
