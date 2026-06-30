package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "deerflow_tool_executions", indexes = {
        @Index(name = "idx_tool_exec_run_id_seq", columnList = "run_id, sequence_no")
})
public class ToolExecutionEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "tool_name", length = 128)
    private String toolName;

    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private Status status;

    @Column(name = "input_json", length = 4000)
    private String inputJson;

    @Column(name = "output", length = 20000)
    private String output;

    @Column(name = "error", length = 4000)
    private String error;

    @Column(name = "metadata_json", length = 4000)
    private String metadataJson;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    public enum Status {
        STARTED, COMPLETED, FAILED, DENIED
    }

    public ToolExecutionEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getInputJson() {
        return inputJson;
    }

    public void setInputJson(String inputJson) {
        this.inputJson = inputJson;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
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

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }
}
