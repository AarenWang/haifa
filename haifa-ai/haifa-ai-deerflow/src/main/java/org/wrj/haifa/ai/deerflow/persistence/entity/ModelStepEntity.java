package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "deerflow_model_steps", indexes = {
        @Index(name = "idx_model_steps_run_id_step", columnList = "run_id, step_index")
})
public class ModelStepEntity {

    @Id
    @Column(name = "step_id", length = 64, nullable = false)
    private String stepId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "step_index", nullable = false)
    private Integer stepIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private Status status;

    @Column(name = "prompt_summary", length = 4000)
    private String promptSummary;

    @Column(name = "response_text", length = 20000)
    private String responseText;

    @Column(name = "finish_reason", length = 64)
    private String finishReason;

    @Column(name = "metadata_json", length = 4000)
    private String metadataJson;

    @Column(name = "token_usage_json", length = 2000)
    private String tokenUsageJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    public ModelStepEntity() {
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
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

    public Integer getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(Integer stepIndex) {
        this.stepIndex = stepIndex;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPromptSummary() {
        return promptSummary;
    }

    public void setPromptSummary(String promptSummary) {
        this.promptSummary = promptSummary;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getTokenUsageJson() {
        return tokenUsageJson;
    }

    public void setTokenUsageJson(String tokenUsageJson) {
        this.tokenUsageJson = tokenUsageJson;
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
