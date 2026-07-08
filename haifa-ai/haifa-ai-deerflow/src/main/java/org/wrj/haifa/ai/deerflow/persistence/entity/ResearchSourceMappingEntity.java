package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "deerflow_research_source_mappings", indexes = {
        @Index(name = "idx_source_map_run_id", columnList = "run_id"),
        @Index(name = "idx_source_map_thread_id", columnList = "thread_id"),
        @Index(name = "idx_source_map_source_id", columnList = "source_id")
})
public class ResearchSourceMappingEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "source_id", length = 64, nullable = false)
    private String sourceId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    public ResearchSourceMappingEntity() {}

    public ResearchSourceMappingEntity(String id, String sourceId, String threadId, String runId) {
        this.id = id;
        this.sourceId = sourceId;
        this.threadId = threadId;
        this.runId = runId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
