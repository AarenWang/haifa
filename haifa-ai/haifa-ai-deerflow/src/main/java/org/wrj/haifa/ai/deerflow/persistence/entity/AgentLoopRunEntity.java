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
@Table(name = "deerflow_agent_loop_runs", indexes = {
        @Index(name = "idx_loop_run_id", columnList = "run_id")
})
public class AgentLoopRunEntity {

    @Id
    @Column(name = "loop_id", length = 64, nullable = false)
    private String loopId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private Status status;

    @Column(name = "max_steps")
    private Integer maxSteps;

    @Column(name = "max_tool_calls")
    private Integer maxToolCalls;

    @Column(name = "timeout_ms")
    private Long timeoutMs;

    @Column(name = "current_step")
    private Integer currentStep;

    @Column(name = "stop_reason", length = 64)
    private String stopReason;

    @Column(name = "metadata_json", length = 4000)
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, TIMEOUT
    }

    public AgentLoopRunEntity() {
    }

    public String getLoopId() {
        return loopId;
    }

    public void setLoopId(String loopId) {
        this.loopId = loopId;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(Integer maxSteps) {
        this.maxSteps = maxSteps;
    }

    public Integer getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(Integer maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
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
