package org.wrj.haifa.ai.deerflow.voice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "voice_turns")
public class VoiceTurnEntity {

    @Id
    @Column(name = "turn_id", length = 64)
    private String turnId;

    @Column(name = "voice_session_id", length = 64, nullable = false)
    private String voiceSessionId;

    @Column(name = "run_id", length = 64)
    private String runId;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "input_duration_ms")
    private Long inputDurationMs;

    @Column(name = "transcript_char_count")
    private Integer transcriptCharCount;

    @Column(name = "output_char_count")
    private Integer outputCharCount;

    @Column(name = "asr_provider", length = 64, nullable = false)
    private String asrProvider;

    @Column(name = "asr_model", length = 128)
    private String asrModel;

    @Column(name = "tts_provider", length = 64, nullable = false)
    private String ttsProvider;

    @Column(name = "tts_model", length = 128)
    private String ttsModel;

    @Column(name = "first_asr_ms")
    private Long firstAsrMs;

    @Column(name = "final_asr_ms")
    private Long finalAsrMs;

    @Column(name = "first_agent_token_ms")
    private Long firstAgentTokenMs;

    @Column(name = "first_tts_audio_ms")
    private Long firstTtsAudioMs;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getTurnId() { return turnId; }
    public void setTurnId(String turnId) { this.turnId = turnId; }
    public String getVoiceSessionId() { return voiceSessionId; }
    public void setVoiceSessionId(String voiceSessionId) { this.voiceSessionId = voiceSessionId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getInputDurationMs() { return inputDurationMs; }
    public void setInputDurationMs(Long inputDurationMs) { this.inputDurationMs = inputDurationMs; }
    public Integer getTranscriptCharCount() { return transcriptCharCount; }
    public void setTranscriptCharCount(Integer transcriptCharCount) { this.transcriptCharCount = transcriptCharCount; }
    public Integer getOutputCharCount() { return outputCharCount; }
    public void setOutputCharCount(Integer outputCharCount) { this.outputCharCount = outputCharCount; }
    public String getAsrProvider() { return asrProvider; }
    public void setAsrProvider(String asrProvider) { this.asrProvider = asrProvider; }
    public String getAsrModel() { return asrModel; }
    public void setAsrModel(String asrModel) { this.asrModel = asrModel; }
    public String getTtsProvider() { return ttsProvider; }
    public void setTtsProvider(String ttsProvider) { this.ttsProvider = ttsProvider; }
    public String getTtsModel() { return ttsModel; }
    public void setTtsModel(String ttsModel) { this.ttsModel = ttsModel; }
    public Long getFirstAsrMs() { return firstAsrMs; }
    public void setFirstAsrMs(Long firstAsrMs) { this.firstAsrMs = firstAsrMs; }
    public Long getFinalAsrMs() { return finalAsrMs; }
    public void setFinalAsrMs(Long finalAsrMs) { this.finalAsrMs = finalAsrMs; }
    public Long getFirstAgentTokenMs() { return firstAgentTokenMs; }
    public void setFirstAgentTokenMs(Long firstAgentTokenMs) { this.firstAgentTokenMs = firstAgentTokenMs; }
    public Long getFirstTtsAudioMs() { return firstTtsAudioMs; }
    public void setFirstTtsAudioMs(Long firstTtsAudioMs) { this.firstTtsAudioMs = firstTtsAudioMs; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
