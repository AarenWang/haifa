package org.wrj.haifa.ai.deerflow.claim;

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
@Table(name = "deerflow_claims")
public class Claim {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @Column(name = "claim_id", length = 64, nullable = false)
    private String claimId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "artifact_id", length = 64)
    private String artifactId;

    @Lob
    @Column(name = "text", nullable = false)
    private String text;

    @Lob
    @Column(name = "support_evidence_ids_json")
    private String supportEvidenceIdsJson;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "status", length = 64, nullable = false)
    private String status; // draft | supported | weakly_supported | unsupported | removed

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Claim() {
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
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

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSupportEvidenceIdsJson() {
        return supportEvidenceIdsJson;
    }

    public void setSupportEvidenceIdsJson(String supportEvidenceIdsJson) {
        this.supportEvidenceIdsJson = supportEvidenceIdsJson;
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

    public List<String> getSupportEvidenceIds() {
        if (supportEvidenceIdsJson == null || supportEvidenceIdsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(supportEvidenceIdsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setSupportEvidenceIds(List<String> list) {
        try {
            this.supportEvidenceIdsJson = MAPPER.writeValueAsString(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            this.supportEvidenceIdsJson = "[]";
        }
    }
}
