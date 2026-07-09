package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchProgressTracker;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchTask;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchTaskStatus;
import org.wrj.haifa.ai.deerflow.todo.TodoItem;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;

@Component
public class TodoSyncNode implements AsyncNodeAction {

    private final ResearchPlanStore planStore;
    private final ResearchProgressTracker progressTracker;
    private final TodoStore todoStore;

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    @Autowired
    public TodoSyncNode(ResearchPlanStore planStore,
                        ResearchProgressTracker progressTracker,
                        ObjectProvider<TodoStore> todoStoreProvider) {
        this(planStore, progressTracker, todoStoreProvider.getIfAvailable());
    }

    public TodoSyncNode(ResearchPlanStore planStore,
                        ResearchProgressTracker progressTracker,
                        TodoStore todoStore) {
        this.planStore = planStore;
        this.progressTracker = progressTracker;
        this.todoStore = todoStore;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null
                ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");
            ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
            if (plan == null) {
                return Map.of(AgentGraphStateKeys.MODEL_STEPS,
                        List.of(Map.of("node", "todo_sync", "status", "skipped")));
            }

            progressTracker.syncTasksFromPlan(runId);
            List<ResearchTask> tasks = planStore.findTasksByRunId(runId);
            List<TodoItem> todos = tasks.stream()
                    .map(task -> toTodo(task, tasks.indexOf(task)))
                    .toList();
            if (todoStore != null) {
                todoStore.saveTodos(threadId, runId, todos);
            }

            Map<String, Object> todoState = new HashMap<>();
            todoState.put("runId", runId);
            todoState.put("items", todos.stream().map(TodoSyncNode::toMap).toList());

            GraphEventRegistry.publish(runId, AgentEvent.of(
                    java.util.UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.TODO_CREATED,
                    "Research todo list synchronized from plan dimensions",
                    Map.of("todoCount", todos.size())
            ));

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.TODOS, todoState);
            update.put(AgentGraphStateKeys.RESEARCH_PHASE, "todo_sync");
            update.put(AgentGraphStateKeys.MODEL_STEPS,
                    List.of(Map.of("node", "todo_sync", "status", "completed", "todoCount", todos.size())));
            return update;
        }, executor);
    }

    private static TodoItem toTodo(ResearchTask task, int index) {
        TodoItem item = new TodoItem(task.id(), task.title(), todoStatus(task.status()));
        item.setPriority("medium");
        item.setEvidence(String.join(",", task.evidenceIds()));
        item.setOrderIndex(index);
        item.setUpdatedAt(Instant.now());
        return item;
    }

    private static Map<String, Object> toMap(TodoItem item) {
        return Map.of(
                "id", item.getId(),
                "content", item.getContent(),
                "status", item.getStatus(),
                "priority", item.getPriority(),
                "orderIndex", item.getOrderIndex() == null ? 0 : item.getOrderIndex()
        );
    }

    private static String todoStatus(ResearchTaskStatus status) {
        if (status == ResearchTaskStatus.COMPLETED) {
            return "completed";
        }
        if (status == ResearchTaskStatus.IN_PROGRESS) {
            return "in_progress";
        }
        return "pending";
    }
}
