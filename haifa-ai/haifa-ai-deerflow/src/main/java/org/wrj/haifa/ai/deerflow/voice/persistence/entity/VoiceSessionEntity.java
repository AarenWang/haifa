package org.wrj.haifa.ai.deerflow.voice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "voice_sessions")
public class VoiceSessionEntity {

    @Id
    @Column(name = "voice_session_id", length = 64)
    private String voiceSessionId;

    @Column(name = "thread_id", length = 64, nullable = false)
    private String threadId;

    @Column(name = "user_id", length = 128, nullable = false)
    private String userId;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "locale", length = 32)
    private String locale;

    @Column(name = "asr_provider", length = 64, nullable = false)
    private String asrProvider;

    @Column(name = "tts_provider", length = 64, nullable = false)
    private String ttsProvider;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    public String getVoiceSessionId() { return voiceSessionId; }
    public void setVoiceSessionId(String voiceSessionId) { this.voiceSessionId = voiceSessionId; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getAsrProvider() { return asrProvider; }
    public void setAsrProvider(String asrProvider) { this.asrProvider = asrProvider; }
    public String getTtsProvider() { return ttsProvider; }
    public void setTtsProvider(String ttsProvider) { this.ttsProvider = ttsProvider; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
