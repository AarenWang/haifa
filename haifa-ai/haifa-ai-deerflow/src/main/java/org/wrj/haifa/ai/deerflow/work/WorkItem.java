package org.wrj.haifa.ai.deerflow.work;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deerflow_work_items")
public class WorkItem {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @Column(name = "work_item_id", length = 64, nullable = false)
    private String workItemId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "parent_work_item_id", length = 64)
    private String parentWorkItemId;

    @Column(name = "kind", length = 64, nullable = false)
    private String kind; // research_question | source_review | synthesis | verification | coding_step | file_edit | user_clarification | other

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Lob
    @Column(name = "goal")
    private String goal;

    @Column(name = "status", length = 64, nullable = false)
    private String status; // proposed | active | blocked | completed | failed | skipped

    @Column(name = "owner", length = 64)
    private String owner; // parent_agent | subagent | tool | human

    @Column(name = "priority", length = 32)
    private String priority;

    @Lob
    @Column(name = "evidence_ids_json")
    private String evidenceIdsJson;

    @Lob
    @Column(name = "artifact_ids_json")
    private String artifactIdsJson;

    @Column(name = "blocked_reason", length = 1000)
    private String blockedReason;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "budget_used")
    private Long budgetUsed;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public WorkItem() {
    }

    public String getWorkItemId() {
        return workItemId;
    }

    public void setWorkItemId(String workItemId) {
        this.workItemId = workItemId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getParentWorkItemId() {
        return parentWorkItemId;
    }

    public void setParentWorkItemId(String parentWorkItemId) {
        this.parentWorkItemId = parentWorkItemId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getEvidenceIdsJson() {
        return evidenceIdsJson;
    }

    public void setEvidenceIdsJson(String evidenceIdsJson) {
        this.evidenceIdsJson = evidenceIdsJson;
    }

    public String getArtifactIdsJson() {
        return artifactIdsJson;
    }

    public void setArtifactIdsJson(String artifactIdsJson) {
        this.artifactIdsJson = artifactIdsJson;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public void setBlockedReason(String blockedReason) {
        this.blockedReason = blockedReason;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Long getBudgetUsed() {
        return budgetUsed;
    }

    public void setBudgetUsed(Long budgetUsed) {
        this.budgetUsed = budgetUsed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<String> getEvidenceIds() {
        if (evidenceIdsJson == null || evidenceIdsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(evidenceIdsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setEvidenceIds(List<String> list) {
        try {
            this.evidenceIdsJson = MAPPER.writeValueAsString(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            this.evidenceIdsJson = "[]";
        }
    }

    public List<String> getArtifactIds() {
        if (artifactIdsJson == null || artifactIdsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(artifactIdsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setArtifactIds(List<String> list) {
        try {
            this.artifactIdsJson = MAPPER.writeValueAsString(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            this.artifactIdsJson = "[]";
        }
    }
}
