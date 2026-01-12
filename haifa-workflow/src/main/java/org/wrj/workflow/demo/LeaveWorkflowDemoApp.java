package org.wrj.workflow.demo;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;

import java.util.HashMap;
import java.util.Map;

public final class LeaveWorkflowDemoApp {
    private static final String LEAVE_PROCESS_KEY = "leaveRequest";

    private LeaveWorkflowDemoApp() {
    }

    public static void main(String[] args) {
        ProcessEngine engine = WorkflowSupport.createInMemoryEngine();
        try {
            WorkflowSupport.deployProcesses(engine);
            runLeaveScenario(engine);
        } finally {
            engine.close();
        }
    }

    private static void runLeaveScenario(ProcessEngine engine) {
        RuntimeService runtimeService = engine.getRuntimeService();
        TaskService taskService = engine.getTaskService();

        Map<String, Object> variables = new HashMap<>();
        variables.put("employee", "zhangsan");
        variables.put("days", 3);
        variables.put("leaveType", "marriage");
        variables.put("reason", "wedding");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(LEAVE_PROCESS_KEY, variables);
        System.out.println("Leave workflow started: " + processInstance.getProcessInstanceId());

        // 依次完成提交、主管审批、证明、总监审批、归档
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of());
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of("approved", true));
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of("certificate", "marriage-proof"));
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of("directorApproved", true));
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of());

        boolean ended = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult() == null;
        System.out.println("Leave workflow ended: " + ended);
    }
}
