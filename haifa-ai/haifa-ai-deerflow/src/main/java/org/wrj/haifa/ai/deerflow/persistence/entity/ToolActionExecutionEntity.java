package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.wrj.haifa.ai.deerflow.tool.execution.ToolExecutionStatus;

@Entity
@Table(name = "deerflow_tool_action_executions", indexes = {
        @Index(name = "idx_tool_action_run_call", columnList = "run_id, tool_call_id")
})
public class ToolActionExecutionEntity {

    @Id
    @Column(name = "idempotency_key", length = 64, nullable = false)
    private String idempotencyKey;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "tool_call_id", length = 128, nullable = false)
    private String toolCallId;

    @Column(name = "normalized_tool_name", length = 128, nullable = false)
    private String normalizedToolName;

    @Column(name = "args_hash", length = 64, nullable = false)
    private String argsHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ToolExecutionStatus status;

    @Column(name = "result", length = 20000)
    private String result;

    @Column(name = "error", length = 4000)
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
    public String getRunId() { return runId; }
    public void setRunId(String value) { this.runId = value; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String value) { this.toolCallId = value; }
    public String getNormalizedToolName() { return normalizedToolName; }
    public void setNormalizedToolName(String value) { this.normalizedToolName = value; }
    public String getArgsHash() { return argsHash; }
    public void setArgsHash(String value) { this.argsHash = value; }
    public ToolExecutionStatus getStatus() { return status; }
    public void setStatus(ToolExecutionStatus value) { this.status = value; }
    public String getResult() { return result; }
    public void setResult(String value) { this.result = value; }
    public String getError() { return error; }
    public void setError(String value) { this.error = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}
