package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "deerflow_memory_candidates", indexes = {
        @Index(name = "idx_mem_candidates_user_status", columnList = "user_id, status")
})
public class MemoryCandidateEntity {

    @Id
    @Column(name = "candidate_id", length = 64, nullable = false)
    private String id;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "agent_id", length = 64)
    private String agentId;

    @Column(name = "category", length = 64, nullable = false)
    private String category;

    @Column(name = "content", length = 20000, nullable = false)
    private String content;

    @Column(name = "source", length = 128)
    private String source;

    @Column(name = "source_thread_id", length = 64)
    private String sourceThreadId;

    @Column(name = "source_run_id", length = 64)
    private String sourceRunId;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "action", length = 32, nullable = false)
    private String action;

    @Column(name = "target_fact_id", length = 64)
    private String targetFactId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "source_error", length = 2000)
    private String sourceError;

    public MemoryCandidateEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceThreadId() {
        return sourceThreadId;
    }

    public void setSourceThreadId(String sourceThreadId) {
        this.sourceThreadId = sourceThreadId;
    }

    public String getSourceRunId() {
        return sourceRunId;
    }

    public void setSourceRunId(String sourceRunId) {
        this.sourceRunId = sourceRunId;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetFactId() {
        return targetFactId;
    }

    public void setTargetFactId(String targetFactId) {
        this.targetFactId = targetFactId;
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

    public String getSourceError() {
        return sourceError;
    }

    public void setSourceError(String sourceError) {
        this.sourceError = sourceError;
    }
}
