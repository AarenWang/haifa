package org.wrj.haifa.ai.deerflow.research.plan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.persistence.entity.ResearchPlanEntity;
import org.wrj.haifa.ai.deerflow.persistence.entity.ResearchTaskEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.ResearchPlanRepository;
import org.wrj.haifa.ai.deerflow.persistence.repository.ResearchTaskRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Primary
public class SQLiteResearchPlanStore implements ResearchPlanStore {

    private final ResearchPlanRepository planRepository;
    private final ResearchTaskRepository taskRepository;
    private final ObjectMapper mapper;

    public SQLiteResearchPlanStore(ResearchPlanRepository planRepository, ResearchTaskRepository taskRepository, ObjectMapper mapper) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ResearchPlan save(ResearchPlan plan) {
        if (plan == null) {
            return null;
        }
        try {
            String json = mapper.writeValueAsString(plan);
            ResearchPlanEntity entity = new ResearchPlanEntity(
                    plan.planId(),
                    plan.threadId(),
                    plan.runId(),
                    json,
                    plan.createdAt(),
                    plan.updatedAt()
            );
            planRepository.save(entity);
            return plan;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save research plan", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResearchPlan> findByPlanId(String planId) {
        if (planId == null) {
            return Optional.empty();
        }
        return planRepository.findById(planId).map(this::deserializePlan);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResearchPlan> findByRunId(String runId) {
        if (runId == null) {
            return Optional.empty();
        }
        return planRepository.findByRunId(runId).map(this::deserializePlan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchPlan> findByThreadId(String threadId) {
        if (threadId == null) {
            return List.of();
        }
        return planRepository.findByThreadId(threadId).stream()
                .map(this::deserializePlan)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByPlanId(String planId) {
        if (planId != null) {
            planRepository.deleteById(planId);
        }
    }

    @Override
    @Transactional
    public ResearchTask saveTask(ResearchTask task) {
        if (task == null) {
            return null;
        }
        try {
            String json = mapper.writeValueAsString(task.evidenceIds());
            ResearchTaskEntity entity = new ResearchTaskEntity(
                    task.id(),
                    task.threadId(),
                    task.runId(),
                    task.title(),
                    task.dimension(),
                    task.status().name(),
                    json
            );
            taskRepository.save(entity);
            return task;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save research task", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResearchTask> findTaskById(String taskId) {
        if (taskId == null) {
            return Optional.empty();
        }
        return taskRepository.findById(taskId).map(this::deserializeTask);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchTask> findTasksByRunId(String runId) {
        if (runId == null) {
            return List.of();
        }
        return taskRepository.findByRunId(runId).stream()
                .map(this::deserializeTask)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchTask> findTasksByDimension(String runId, String dimension) {
        if (runId == null || dimension == null) {
            return List.of();
        }
        return findTasksByRunId(runId).stream()
                .filter(t -> dimension.equals(t.dimension()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTaskById(String taskId) {
        if (taskId != null) {
            taskRepository.deleteById(taskId);
        }
    }

    private ResearchPlan deserializePlan(ResearchPlanEntity entity) {
        try {
            return mapper.readValue(entity.getFullPlanJson(), ResearchPlan.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize research plan", e);
        }
    }

    private ResearchTask deserializeTask(ResearchTaskEntity entity) {
        try {
            List<String> evidenceIds = mapper.readValue(entity.getEvidenceIdsJson(), new TypeReference<List<String>>() {});
            return new ResearchTask(
                    entity.getId(),
                    entity.getThreadId(),
                    entity.getTitle(),
                    entity.getDimension(),
                    ResearchTaskStatus.valueOf(entity.getStatus()),
                    evidenceIds,
                    entity.getRunId()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize research task", e);
        }
    }
}
