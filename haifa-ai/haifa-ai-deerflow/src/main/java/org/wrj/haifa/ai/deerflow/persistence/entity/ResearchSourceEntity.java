package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "deerflow_research_sources")
public class ResearchSourceEntity {

    @Id
    @Column(name = "source_id", length = 64, nullable = false)
    private String sourceId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "normalized_url", length = 2048, nullable = false)
    private String normalizedUrl;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Lob
    @Column(name = "full_source_json", nullable = false)
    private String fullSourceJson;

    @Lob
    @Column(name = "raw_content")
    private String rawContent;

    @Lob
    @Column(name = "extracted_content_json")
    private String extractedContentJson;

    @Column(name = "fetched", nullable = false)
    private boolean fetched;

    public ResearchSourceEntity() {}

    public ResearchSourceEntity(String sourceId, String threadId, String runId, String normalizedUrl, String contentHash, String fullSourceJson, String rawContent, String extractedContentJson, boolean fetched) {
        this.sourceId = sourceId;
        this.threadId = threadId;
        this.runId = runId;
        this.normalizedUrl = normalizedUrl;
        this.contentHash = contentHash;
        this.fullSourceJson = fullSourceJson;
        this.rawContent = rawContent;
        this.extractedContentJson = extractedContentJson;
        this.fetched = fetched;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
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

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public void setNormalizedUrl(String normalizedUrl) {
        this.normalizedUrl = normalizedUrl;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getFullSourceJson() {
        return fullSourceJson;
    }

    public void setFullSourceJson(String fullSourceJson) {
        this.fullSourceJson = fullSourceJson;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getExtractedContentJson() {
        return extractedContentJson;
    }

    public void setExtractedContentJson(String extractedContentJson) {
        this.extractedContentJson = extractedContentJson;
    }

    public boolean isFetched() {
        return fetched;
    }

    public void setFetched(boolean fetched) {
        this.fetched = fetched;
    }
}
