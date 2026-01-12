package org.wrj.workflow.demo;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;

import java.util.HashMap;
import java.util.Map;

public final class LoanWorkflowDemoApp {
    private static final String LOAN_PROCESS_KEY = "loanApproval";

    private LoanWorkflowDemoApp() {
    }

    public static void main(String[] args) {
        ProcessEngine engine = WorkflowSupport.createInMemoryEngine();
        try {
            WorkflowSupport.deployProcesses(engine);
            runLoanScenario(engine);
        } finally {
            engine.close();
        }
    }

    private static void runLoanScenario(ProcessEngine engine) {
        RuntimeService runtimeService = engine.getRuntimeService();
        TaskService taskService = engine.getTaskService();

        Map<String, Object> variables = new HashMap<>();
        variables.put("applicant", "lisi");
        variables.put("loanAmount", 500_000);
        variables.put("income", 30_000);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(LOAN_PROCESS_KEY, variables);
        System.out.println("Loan workflow started: " + processInstance.getProcessInstanceId());

        // 依次完成申请、信审、风控、签约
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of());
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of("creditScore", 720));
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(),
                Map.of("approved", true, "riskLevel", "LOW"));
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of("contractId", "LN-2024-001"));

        boolean ended = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult() == null;
        System.out.println("Loan workflow ended: " + ended);
    }
}
