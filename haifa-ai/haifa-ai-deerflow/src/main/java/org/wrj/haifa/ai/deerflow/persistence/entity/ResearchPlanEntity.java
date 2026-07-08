package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "deerflow_research_plans")
public class ResearchPlanEntity {

    @Id
    @Column(name = "plan_id", length = 64, nullable = false)
    private String planId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Lob
    @Column(name = "full_plan_json", nullable = false)
    private String fullPlanJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ResearchPlanEntity() {}

    public ResearchPlanEntity(String planId, String threadId, String runId, String fullPlanJson, Instant createdAt, Instant updatedAt) {
        this.planId = planId;
        this.threadId = threadId;
        this.runId = runId;
        this.fullPlanJson = fullPlanJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getFullPlanJson() {
        return fullPlanJson;
    }

    public void setFullPlanJson(String fullPlanJson) {
        this.fullPlanJson = fullPlanJson;
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
}
