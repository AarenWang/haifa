package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "agent_graph_checkpoint_external_refs")
public class AgentGraphCheckpointExternalRefEntity {

    @Id
    @Column(name = "ref_id", length = 64, nullable = false)
    private String refId;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AgentGraphCheckpointExternalRefEntity() {}

    public AgentGraphCheckpointExternalRefEntity(String refId, String content, Instant createdAt) {
        this.refId = refId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
