package org.wrj.haifa.ai.deerflow.voice.domain;

import java.time.Instant;

public class VoiceSession {
    private final String voiceSessionId;
    private final String threadId;
    private final String userId;
    private VoiceSessionStatus status;
    private String locale;
    private String asrProvider;
    private String ttsProvider;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;

    public VoiceSession(String voiceSessionId, String threadId, String userId, String asrProvider, String ttsProvider) {
        this.voiceSessionId = voiceSessionId;
        this.threadId = threadId;
        this.userId = userId;
        this.status = VoiceSessionStatus.ACTIVE;
        this.locale = "zh-CN";
        this.asrProvider = asrProvider;
        this.ttsProvider = ttsProvider;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getVoiceSessionId() { return voiceSessionId; }
    public String getThreadId() { return threadId; }
    public String getUserId() { return userId; }
    public VoiceSessionStatus getStatus() { return status; }
    public void setStatus(VoiceSessionStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
        if (status == VoiceSessionStatus.CLOSED) {
            this.closedAt = Instant.now();
        }
    }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; this.updatedAt = Instant.now(); }
    public String getAsrProvider() { return asrProvider; }
    public void setAsrProvider(String asrProvider) { this.asrProvider = asrProvider; this.updatedAt = Instant.now(); }
    public String getTtsProvider() { return ttsProvider; }
    public void setTtsProvider(String ttsProvider) { this.ttsProvider = ttsProvider; this.updatedAt = Instant.now(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getClosedAt() { return closedAt; }
}
