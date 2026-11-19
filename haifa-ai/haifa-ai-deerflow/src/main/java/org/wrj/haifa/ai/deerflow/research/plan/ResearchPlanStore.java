package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.List;
import java.util.Optional;

/**
 * Storage interface for research plans and tasks.
 */
public interface ResearchPlanStore {

    ResearchPlan save(ResearchPlan plan);

    Optional<ResearchPlan> findByPlanId(String planId);

    Optional<ResearchPlan> findByRunId(String runId);

    List<ResearchPlan> findByThreadId(String threadId);

    void deleteByPlanId(String planId);

    // Tasks
    ResearchTask saveTask(ResearchTask task);

    Optional<ResearchTask> findTaskById(String taskId);

    List<ResearchTask> findTasksByRunId(String runId);

    List<ResearchTask> findTasksByDimension(String runId, String dimension);

    void deleteTaskById(String taskId);
}
