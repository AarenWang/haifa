package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

@Component
public class ResearchDimensionDispatchNode implements AsyncNodeAction {

    private final ResearchPlanStore planStore;
    private final ResearchProgressTracker progressTracker;

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    public ResearchDimensionDispatchNode(ResearchPlanStore planStore,
                                         ResearchProgressTracker progressTracker) {
        this.planStore = planStore;
        this.progressTracker = progressTracker;
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
                        List.of(Map.of("node", "dispatch_dimensions", "status", "skipped")));
            }

            progressTracker.syncTasksFromPlan(runId);
            List<ResearchTask> tasks = planStore.findTasksByRunId(runId);
            List<Map<String, Object>> dispatchedTasks = tasks.stream()
                    .map(task -> Map.<String, Object>of(
                            "taskId", task.id(),
                            "title", task.title(),
                            "dimension", task.dimension(),
                            "status", task.status().name(),
                            "dispatchMode", "dimension_graph"
                    ))
                    .toList();

            tasks.stream()
                    .filter(task -> task.status() == ResearchTaskStatus.PENDING
                            || task.status() == ResearchTaskStatus.IN_PROGRESS)
                    .forEach(task -> GraphEventRegistry.publish(runId, AgentEvent.of(
                            UUID.randomUUID().toString(),
                            runId,
                            threadId,
                            AgentEventType.RESEARCH_STEP_COMPLETED,
                            "Research dimension dispatched: " + task.title(),
                            Map.of("taskId", task.id(),
                                   "dimension", task.dimension(),
                                   "dispatchMode", "dimension_graph")
                    )));

            Map<String, Object> subagents = new HashMap<>();
            subagents.put("researchDimensions", dispatchedTasks);
            subagents.put("dispatchMode", "dimension_graph");

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.SUBAGENTS, subagents);
            update.put(AgentGraphStateKeys.RESEARCH_PHASE, "dimension_dispatch");
            update.put(AgentGraphStateKeys.MODEL_STEPS,
                    List.of(Map.of("node", "dispatch_dimensions", "status", "completed", "taskCount", tasks.size())));
            return update;
        }, executor);
    }
}
