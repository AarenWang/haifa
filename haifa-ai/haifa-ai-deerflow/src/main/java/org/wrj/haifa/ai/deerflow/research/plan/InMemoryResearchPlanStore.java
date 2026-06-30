package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * In-memory implementation of research plan and task storage.
 */
@Component
public class InMemoryResearchPlanStore implements ResearchPlanStore {

    private final Map<String, ResearchPlan> plansById = new ConcurrentHashMap<>();
    private final Map<String, String> planIdByRunId = new ConcurrentHashMap<>();
    private final Map<String, List<String>> planIdsByThreadId = new ConcurrentHashMap<>();
    private final Map<String, ResearchTask> tasksById = new ConcurrentHashMap<>();
    private final Map<String, List<String>> taskIdsByRunId = new ConcurrentHashMap<>();

    @Override
    public ResearchPlan save(ResearchPlan plan) {
        plansById.put(plan.planId(), plan);
        planIdByRunId.put(plan.runId(), plan.planId());
        planIdsByThreadId.computeIfAbsent(plan.threadId(), k -> new ArrayList<>());
        List<String> planIds = planIdsByThreadId.get(plan.threadId());
        if (!planIds.contains(plan.planId())) {
            planIds.add(plan.planId());
        }
        return plan;
    }

    @Override
    public Optional<ResearchPlan> findByPlanId(String planId) {
        return Optional.ofNullable(plansById.get(planId));
    }

    @Override
    public Optional<ResearchPlan> findByRunId(String runId) {
        String planId = planIdByRunId.get(runId);
        if (planId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(plansById.get(planId));
    }

    @Override
    public List<ResearchPlan> findByThreadId(String threadId) {
        List<String> planIds = planIdsByThreadId.getOrDefault(threadId, List.of());
        List<ResearchPlan> plans = new ArrayList<>();
        for (String planId : planIds) {
            ResearchPlan plan = plansById.get(planId);
            if (plan != null) {
                plans.add(plan);
            }
        }
        return plans;
    }

    @Override
    public void deleteByPlanId(String planId) {
        ResearchPlan plan = plansById.remove(planId);
        if (plan != null) {
            planIdByRunId.remove(plan.runId());
            List<String> threadPlans = planIdsByThreadId.get(plan.threadId());
            if (threadPlans != null) {
                threadPlans.remove(planId);
            }
        }
    }

    @Override
    public ResearchTask saveTask(ResearchTask task) {
        tasksById.put(task.id(), task);
        taskIdsByRunId.computeIfAbsent(task.runId(), k -> new ArrayList<>());
        List<String> taskIds = taskIdsByRunId.get(task.runId());
        if (!taskIds.contains(task.id())) {
            taskIds.add(task.id());
        }
        return task;
    }

    @Override
    public Optional<ResearchTask> findTaskById(String taskId) {
        return Optional.ofNullable(tasksById.get(taskId));
    }

    @Override
    public List<ResearchTask> findTasksByRunId(String runId) {
        List<String> taskIds = taskIdsByRunId.getOrDefault(runId, List.of());
        List<ResearchTask> tasks = new ArrayList<>();
        for (String taskId : taskIds) {
            ResearchTask task = tasksById.get(taskId);
            if (task != null) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    @Override
    public List<ResearchTask> findTasksByDimension(String runId, String dimension) {
        return findTasksByRunId(runId).stream()
                .filter(t -> t.dimension().equals(dimension))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteTaskById(String taskId) {
        ResearchTask task = tasksById.remove(taskId);
        if (task != null) {
            List<String> runTasks = taskIdsByRunId.get(task.runId());
            if (runTasks != null) {
                runTasks.remove(taskId);
            }
        }
    }
}
