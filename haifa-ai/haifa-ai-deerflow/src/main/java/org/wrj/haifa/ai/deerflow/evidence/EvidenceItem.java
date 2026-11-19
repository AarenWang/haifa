package org.wrj.haifa.ai.deerflow.evidence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity(name = "EvidenceItemV2")
@Table(name = "deerflow_evidence_items_v2")
public class EvidenceItem {

    @Id
    @Column(name = "evidence_id", length = 64, nullable = false)
    private String evidenceId;

    @Column(name = "source_id", length = 64, nullable = false)
    private String sourceId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "work_item_id", length = 64)
    private String workItemId;

    @Lob
    @Column(name = "summary")
    private String summary;

    @Lob
    @Column(name = "claim_support_text")
    private String claimSupportText;

    @Column(name = "locator", length = 1000)
    private String locator;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "accepted")
    private Boolean accepted;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "created_at")
    private Instant createdAt;

    public EvidenceItem() {
    }

    public String getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(String evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
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

    public String getWorkItemId() {
        return workItemId;
    }

    public void setWorkItemId(String workItemId) {
        this.workItemId = workItemId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getClaimSupportText() {
        return claimSupportText;
    }

    public void setClaimSupportText(String claimSupportText) {
        this.claimSupportText = claimSupportText;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
