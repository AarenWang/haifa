package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;

@Entity
@Table(name = "deerflow_messages", indexes = {
        @Index(name = "idx_messages_thread_id_seq", columnList = "thread_id, sequence_no"),
        @Index(name = "idx_messages_run_id", columnList = "run_id")
})
public class MessageEntity {

    @Id
    @Column(name = "message_id", length = 64, nullable = false)
    private String messageId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "run_id", length = 64)
    private String runId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 32)
    private MessageRole role;

    @Column(name = "content", length = 20000)
    private String content;

    @Column(name = "metadata_json", length = 4000)
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    public MessageEntity() {
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
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
