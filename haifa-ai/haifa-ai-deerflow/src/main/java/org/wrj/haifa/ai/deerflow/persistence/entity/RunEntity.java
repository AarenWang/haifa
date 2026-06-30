package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.wrj.haifa.ai.deerflow.run.RunStatus;

@Entity
@Table(name = "deerflow_runs", indexes = {
        @Index(name = "idx_runs_thread_id_created_at", columnList = "thread_id, created_at")
})
public class RunEntity {

    @Id
    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "mode", length = 32)
    private String mode;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private RunStatus status;

    @Column(name = "error", length = 4000)
    private String error;

    @Column(name = "metadata_json", length = 4000)
    private String metadataJson;

    @Column(name = "research_options_json", length = 4000)
    private String researchOptionsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RunEntity() {
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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getResearchOptionsJson() {
        return researchOptionsJson;
    }

    public void setResearchOptionsJson(String researchOptionsJson) {
        this.researchOptionsJson = researchOptionsJson;
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
