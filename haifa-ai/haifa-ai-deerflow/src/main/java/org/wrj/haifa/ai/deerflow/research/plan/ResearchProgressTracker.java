package org.wrj.haifa.ai.deerflow.research.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;

/**
 * Tracks research progress by updating dimension and task status as the research loop executes.
 */
@Component
public class ResearchProgressTracker {

    private final ResearchPlanStore planStore;

    public ResearchProgressTracker(ResearchPlanStore planStore) {
        this.planStore = planStore;
    }

    /**
     * Mark a dimension as started and create a corresponding task.
     */
    public void markDimensionStarted(String runId, String dimensionId) {
        ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
        if (plan == null) return;

        List<ResearchDimension> updatedDimensions = new ArrayList<>();
        boolean createdTask = false;
        for (ResearchDimension dim : plan.dimensions()) {
            if (dim.id().equals(dimensionId) && dim.status() == ResearchTaskStatus.PENDING) {
                updatedDimensions.add(dim.withStatus(ResearchTaskStatus.IN_PROGRESS));
                createdTask = true;
            } else {
                updatedDimensions.add(dim);
            }
        }
        ResearchPlan updatedPlan = plan.withDimensions(updatedDimensions).withStatus("IN_PROGRESS");
        planStore.save(updatedPlan);
        if (createdTask && planStore.findTasksByDimension(runId, dimensionId).isEmpty()) {
            ResearchDimension dimension = updatedPlan.dimensions().stream()
                    .filter(dim -> dim.id().equals(dimensionId))
                    .findFirst()
                    .orElse(null);
            if (dimension != null) {
                planStore.saveTask(new ResearchTask(
                        UUID.randomUUID().toString(),
                        updatedPlan.threadId(),
                        dimension.title(),
                        dimension.id(),
                        ResearchTaskStatus.IN_PROGRESS,
                        List.of(),
                        runId
                ));
            }
        }
    }

    /**
     * Mark a dimension as completed.
     */
    public void markDimensionCompleted(String runId, String dimensionId) {
        ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
        if (plan == null) return;

        List<ResearchDimension> updatedDimensions = new ArrayList<>();
        for (ResearchDimension dim : plan.dimensions()) {
            if (dim.id().equals(dimensionId)) {
                updatedDimensions.add(dim.withStatus(ResearchTaskStatus.COMPLETED));
            } else {
                updatedDimensions.add(dim);
            }
        }
        planStore.save(plan.withDimensions(updatedDimensions));

        // Update corresponding task
        List<ResearchTask> tasks = planStore.findTasksByDimension(runId, dimensionId);
        for (ResearchTask task : tasks) {
            if (task.status() != ResearchTaskStatus.COMPLETED) {
                planStore.saveTask(task.withStatus(ResearchTaskStatus.COMPLETED).withEvidenceIds(resolveEvidenceIds(updatedDimensions, dimensionId)));
            }
        }

        if (updatedDimensions.stream().allMatch(dim -> dim.status() == ResearchTaskStatus.COMPLETED)) {
            planStore.save(planStore.findByRunId(runId).orElse(plan).withStatus("COMPLETED"));
        }
    }

    /**
     * Update source and evidence counts for a dimension.
     */
    public void updateDimensionProgress(String runId, String dimensionId, int sourceCount, List<EvidenceItem> evidenceItems) {
        ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
        if (plan == null) return;

        int evidenceCount = evidenceItems == null ? 0 : evidenceItems.size();
        List<String> evidenceIds = evidenceItems == null ? List.of() : evidenceItems.stream()
                .map(EvidenceItem::evidenceId)
                .toList();

        List<ResearchDimension> updatedDimensions = new ArrayList<>();
        for (ResearchDimension dim : plan.dimensions()) {
            if (dim.id().equals(dimensionId)) {
                updatedDimensions.add(dim
                        .withSourceCount(sourceCount)
                        .withEvidenceCount(evidenceCount)
                        .withEvidenceIds(evidenceIds));
            } else {
                updatedDimensions.add(dim);
            }
        }
        planStore.save(plan.withDimensions(updatedDimensions));
        syncTaskEvidence(runId, dimensionId, evidenceIds);
    }

    public void recordFetchedSource(String runId, String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return;
        }
        ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
        if (plan == null) return;
        ResearchDimension active = activeDimension(plan);
        if (active == null) {
            return;
        }
        updateDimensionProgress(runId, active.id(), active.actualSourceCount() + 1, evidenceItemsForDimension(runId, active.id(), active.evidenceIds()));
    }

    public void recordEvidence(String runId, EvidenceItem evidenceItem) {
        if (evidenceItem == null) {
            return;
        }
        ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
        if (plan == null) return;
        ResearchDimension active = activeDimension(plan);
        if (active == null) {
            return;
        }
        List<EvidenceItem> updatedEvidence = new ArrayList<>(evidenceItemsForDimension(runId, active.id(), active.evidenceIds()));
        boolean exists = updatedEvidence.stream().anyMatch(existing -> existing.evidenceId().equals(evidenceItem.evidenceId()));
        if (!exists) {
            updatedEvidence.add(evidenceItem);
        }
        updateDimensionProgress(runId, active.id(), active.actualSourceCount(), updatedEvidence);
    }

    public void syncTasksFromPlan(String runId) {
        ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
        if (plan == null) {
            return;
        }
        for (ResearchDimension dimension : plan.dimensions()) {
            List<ResearchTask> tasks = planStore.findTasksByDimension(runId, dimension.id());
            if (tasks.isEmpty()) {
                planStore.saveTask(new ResearchTask(
                        UUID.randomUUID().toString(),
                        plan.threadId(),
                        dimension.title(),
                        dimension.id(),
                        dimension.status(),
                        dimension.evidenceIds(),
                        runId
                ));
                continue;
            }
            for (ResearchTask task : tasks) {
                planStore.saveTask(task.withStatus(dimension.status()).withEvidenceIds(dimension.evidenceIds()));
            }
        }
    }

    /**
     * Get current progress summary for a run.
     */
    public ResearchProgress getProgress(String runId) {
        ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
        if (plan == null) {
            return ResearchProgress.empty();
        }

        int total = plan.dimensions().size();
        int completed = 0;
        int inProgress = 0;
        int totalSources = 0;
        int totalEvidence = 0;

        for (ResearchDimension dim : plan.dimensions()) {
            if (dim.status() == ResearchTaskStatus.COMPLETED) completed++;
            else if (dim.status() == ResearchTaskStatus.IN_PROGRESS) inProgress++;
            totalSources += dim.actualSourceCount();
            totalEvidence += dim.actualEvidenceCount();
        }

        return new ResearchProgress(total, completed, inProgress, totalSources, totalEvidence, plan.status());
    }

    /**
     * Progress summary for a research run.
     */
    public record ResearchProgress(
            int totalDimensions,
            int completedDimensions,
            int inProgressDimensions,
            int totalSources,
            int totalEvidence,
            String planStatus
    ) {
        public static ResearchProgress empty() {
            return new ResearchProgress(0, 0, 0, 0, 0, "UNKNOWN");
        }

        public double completionPercentage() {
            return totalDimensions == 0 ? 0.0 : (completedDimensions * 100.0 / totalDimensions);
        }
    }

    private ResearchDimension activeDimension(ResearchPlan plan) {
        for (ResearchDimension dimension : plan.dimensions()) {
            if (dimension.status() == ResearchTaskStatus.IN_PROGRESS) {
                return dimension;
            }
        }
        for (ResearchDimension dimension : plan.dimensions()) {
            if (dimension.status() == ResearchTaskStatus.PENDING) {
                return dimension;
            }
        }
        return null;
    }

    private List<EvidenceItem> evidenceItemsForDimension(String runId, String dimensionId, List<String> evidenceIds) {
        if (evidenceIds == null || evidenceIds.isEmpty()) {
            return List.of();
        }
        return planStore.findTasksByDimension(runId, dimensionId).stream()
                .flatMap(task -> task.evidenceIds().stream())
                .distinct()
                .filter(id -> evidenceIds.contains(id))
                .map(id -> new EvidenceItem(id, "", runId, "", "", "", dimensionId, 0.0, null))
                .toList();
    }

    private List<String> resolveEvidenceIds(List<ResearchDimension> dimensions, String dimensionId) {
        return dimensions.stream()
                .filter(dim -> dim.id().equals(dimensionId))
                .findFirst()
                .map(ResearchDimension::evidenceIds)
                .orElse(List.of());
    }

    private void syncTaskEvidence(String runId, String dimensionId, List<String> evidenceIds) {
        for (ResearchTask task : planStore.findTasksByDimension(runId, dimensionId)) {
            planStore.saveTask(task.withEvidenceIds(evidenceIds));
        }
    }
}
