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
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;

@Entity
@Table(name = "deerflow_events", indexes = {
        @Index(name = "idx_events_run_id_seq", columnList = "run_id, sequence_no"),
        @Index(name = "idx_events_thread_id_created_at", columnList = "thread_id, created_at")
})
public class AgentEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "run_id", length = 64, nullable = false)
    private String runId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 64)
    private AgentEventType type;

    @Column(name = "content", length = 20000)
    private String content;

    @Column(name = "metadata_json", length = 4000)
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    public AgentEventEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public AgentEventType getType() {
        return type;
    }

    public void setType(AgentEventType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }
}
