package org.wrj.workflow.demo;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.util.HashMap;
import java.util.Map;

public final class LeaveAdvancedDemoApp {
    private static final String PROCESS_KEY = "leaveAdvanced";

    private LeaveAdvancedDemoApp() {
    }

    public static void main(String[] args) {
        ProcessEngine engine = WorkflowSupport.createInMemoryEngine();
        try {
            deployProcess(engine);
            runAdvancedScenario(engine);
        } finally {
            engine.close();
        }
    }

    private static void deployProcess(ProcessEngine engine) {
        RepositoryService repositoryService = engine.getRepositoryService();
        repositoryService.createDeployment()
                .name("haifa-workflow-advanced-demo")
                .addClasspathResource("processes/leave-advanced.bpmn20.xml")
                .deploy();
    }

    private static void runAdvancedScenario(ProcessEngine engine) {
        RuntimeService runtimeService = engine.getRuntimeService();
        TaskService taskService = engine.getTaskService();

        Map<String, Object> variables = new HashMap<>();
        variables.put("employee", "zhangsan");
        variables.put("days", 2);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
        System.out.println("Leave advanced workflow started: " + processInstance.getProcessInstanceId());

        // 1) 正常提交申请
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of());

        // 2) 加签：给当前审批任务临时增加一个候选人
        Task managerTask = getTaskByKey(taskService, processInstance.getId(), "managerApproval");
        taskService.addCandidateUser(managerTask.getId(), "viceManager");
        System.out.println("Add sign user: viceManager");

        // 3) 驳回：从主管审批回到提交申请
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("managerApproval", "submitRequest")
                .changeState();
        System.out.println("Reject to submitRequest");

        // 4) 重新提交，再次进入主管审批
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of("resubmit", true));

        // 5) 主管审批通过，流程进入 HR 归档
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of("approved", true));

        // 6) 撤回：将已到 HR 的流程拉回主管审批
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdTo("hrRecord", "managerApproval")
                .changeState();
        System.out.println("Withdraw to managerApproval");

        // 7) 主管再次处理并完成归档
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of("approved", true));
        WorkflowSupport.completeSingleTask(taskService, processInstance.getId(), Map.of("archived", true));

        boolean ended = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult() == null;
        System.out.println("Leave advanced workflow ended: " + ended);
    }

    private static Task getTaskByKey(TaskService taskService, String processInstanceId, String taskKey) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskKey)
                .singleResult();
        if (task == null) {
            throw new IllegalStateException("Task not found: " + taskKey);
        }
        return task;
    }
}
