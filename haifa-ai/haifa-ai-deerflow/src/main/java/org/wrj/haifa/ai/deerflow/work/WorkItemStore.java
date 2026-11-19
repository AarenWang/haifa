package org.wrj.haifa.ai.deerflow.work;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkItemStore {

    private final WorkItemRepository repository;

    public WorkItemStore(WorkItemRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WorkItem create(String runId, String threadId, String parentWorkItemId, String kind, String title, String goal, String priority, String owner) {
        WorkItem item = new WorkItem();
        item.setWorkItemId(UUID.randomUUID().toString());
        item.setRunId(runId);
        item.setThreadId(threadId);
        item.setParentWorkItemId(parentWorkItemId);
        item.setKind(kind);
        item.setTitle(title);
        item.setGoal(goal);
        item.setStatus("proposed");
        item.setOwner(owner != null ? owner : "parent_agent");
        item.setPriority(priority != null ? priority : "medium");
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());
        item.setEvidenceIds(List.of());
        item.setArtifactIds(List.of());
        return repository.save(item);
    }

    @Transactional
    public WorkItem updateStatus(String workItemId, String status, String blockedReason, String failureReason) {
        return repository.findById(workItemId).map(item -> {
            item.setStatus(status);
            item.setBlockedReason(blockedReason);
            item.setFailureReason(failureReason);
            item.setUpdatedAt(Instant.now());
            if ("completed".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status) || "skipped".equalsIgnoreCase(status)) {
                item.setCompletedAt(Instant.now());
            }
            return repository.save(item);
        }).orElse(null);
    }

    @Transactional
    public WorkItem addEvidenceId(String workItemId, String evidenceId) {
        return repository.findById(workItemId).map(item -> {
            List<String> evidence = new java.util.ArrayList<>(item.getEvidenceIds());
            if (!evidence.contains(evidenceId)) {
                evidence.add(evidenceId);
                item.setEvidenceIds(evidence);
                item.setUpdatedAt(Instant.now());
            }
            return repository.save(item);
        }).orElse(null);
    }

    @Transactional
    public WorkItem addArtifactId(String workItemId, String artifactId) {
        return repository.findById(workItemId).map(item -> {
            List<String> artifacts = new java.util.ArrayList<>(item.getArtifactIds());
            if (!artifacts.contains(artifactId)) {
                artifacts.add(artifactId);
                item.setArtifactIds(artifacts);
                item.setUpdatedAt(Instant.now());
            }
            return repository.save(item);
        }).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findByRunId(String runId) {
        return repository.findByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findByThreadId(String threadId) {
        return repository.findByThreadId(threadId);
    }
}
