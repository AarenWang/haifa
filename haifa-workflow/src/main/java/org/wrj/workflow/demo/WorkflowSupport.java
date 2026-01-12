package org.wrj.workflow.demo;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;

import java.util.List;
import java.util.Map;

final class WorkflowSupport {
    private WorkflowSupport() {
    }

    static ProcessEngine createInMemoryEngine() {
        // 使用内存型 H2 数据库，方便本地演示
        StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
        config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        config.setAsyncExecutorActivate(false);
        return config.buildProcessEngine();
    }

    static void deployProcesses(ProcessEngine engine) {
        RepositoryService repositoryService = engine.getRepositoryService();
        repositoryService.createDeployment()
                .name("haifa-workflow-demo")
                .addClasspathResource("processes/leave-request.bpmn20.xml")
                .addClasspathResource("processes/loan-approval.bpmn20.xml")
                .deploy();
    }

    static void completeSingleTask(TaskService taskService, String processInstanceId,
                                   Map<String, Object> variables) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        if (tasks.isEmpty()) {
            throw new IllegalStateException("No active task for process instance " + processInstanceId);
        }
        if (tasks.size() > 1) {
            throw new IllegalStateException("Expected one task but found " + tasks.size());
        }

        Task task = tasks.get(0);
        System.out.printf("Completing task: %s (assignee=%s)%n", task.getName(), task.getAssignee());

        if (variables == null || variables.isEmpty()) {
            taskService.complete(task.getId());
        } else {
            taskService.complete(task.getId(), variables);
        }
    }
}
