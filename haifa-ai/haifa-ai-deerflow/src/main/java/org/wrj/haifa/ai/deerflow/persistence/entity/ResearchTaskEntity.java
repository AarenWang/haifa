package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "deerflow_research_tasks")
public class ResearchTaskEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "dimension", length = 128)
    private String dimension;

    @Column(name = "status", length = 64)
    private String status;

    @Lob
    @Column(name = "evidence_ids_json")
    private String evidenceIdsJson;

    public ResearchTaskEntity() {}

    public ResearchTaskEntity(String id, String threadId, String runId, String title, String dimension, String status, String evidenceIdsJson) {
        this.id = id;
        this.threadId = threadId;
        this.runId = runId;
        this.title = title;
        this.dimension = dimension;
        this.status = status;
        this.evidenceIdsJson = evidenceIdsJson;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEvidenceIdsJson() {
        return evidenceIdsJson;
    }

    public void setEvidenceIdsJson(String evidenceIdsJson) {
        this.evidenceIdsJson = evidenceIdsJson;
    }
}
