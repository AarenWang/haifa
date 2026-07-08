package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "deerflow_evidence_items")
public class EvidenceItemEntity {

    @Id
    @Column(name = "evidence_id", length = 64, nullable = false)
    private String evidenceId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "source_id", length = 64, nullable = false)
    private String sourceId;

    @Lob
    @Column(name = "quote_or_paraphrase")
    private String quoteOrParaphrase;

    @Lob
    @Column(name = "claim")
    private String claim;

    @Column(name = "dimension", length = 128)
    private String dimension;

    @Column(name = "confidence")
    private double confidence;

    @Column(name = "extracted_at")
    private Instant extractedAt;

    @Column(name = "signature", length = 512, nullable = false)
    private String signature;

    public EvidenceItemEntity() {}

    public EvidenceItemEntity(String evidenceId, String threadId, String runId, String sourceId, String quoteOrParaphrase, String claim, String dimension, double confidence, Instant extractedAt, String signature) {
        this.evidenceId = evidenceId;
        this.threadId = threadId;
        this.runId = runId;
        this.sourceId = sourceId;
        this.quoteOrParaphrase = quoteOrParaphrase;
        this.claim = claim;
        this.dimension = dimension;
        this.confidence = confidence;
        this.extractedAt = extractedAt;
        this.signature = signature;
    }

    public String getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(String evidenceId) {
        this.evidenceId = evidenceId;
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

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getQuoteOrParaphrase() {
        return quoteOrParaphrase;
    }

    public void setQuoteOrParaphrase(String quoteOrParaphrase) {
        this.quoteOrParaphrase = quoteOrParaphrase;
    }

    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Instant getExtractedAt() {
        return extractedAt;
    }

    public void setExtractedAt(Instant extractedAt) {
        this.extractedAt = extractedAt;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
