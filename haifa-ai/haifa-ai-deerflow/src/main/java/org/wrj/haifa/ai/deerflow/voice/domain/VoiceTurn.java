package org.wrj.haifa.ai.deerflow.voice.domain;

import java.time.Instant;

public class VoiceTurn {
    private final String turnId;
    private final String voiceSessionId;
    private String runId;
    private VoiceTurnStatus status;
    private Long inputDurationMs;
    private Integer transcriptCharCount;
    private Integer outputCharCount;
    private String asrProvider;
    private String asrModel;
    private String ttsProvider;
    private String ttsModel;
    private Long firstAsrMs;
    private Long finalAsrMs;
    private Long firstAgentTokenMs;
    private Long firstTtsAudioMs;
    private String errorCode;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public VoiceTurn(String turnId, String voiceSessionId, String asrProvider, String ttsProvider) {
        this.turnId = turnId;
        this.voiceSessionId = voiceSessionId;
        this.status = VoiceTurnStatus.CREATED;
        this.asrProvider = asrProvider;
        this.ttsProvider = ttsProvider;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getTurnId() { return turnId; }
    public String getVoiceSessionId() { return voiceSessionId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; this.updatedAt = Instant.now(); }
    public VoiceTurnStatus getStatus() { return status; }
    public void setStatus(VoiceTurnStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
        if (status == VoiceTurnStatus.COMPLETED || status == VoiceTurnStatus.CANCELLED || status == VoiceTurnStatus.ERROR) {
            this.completedAt = Instant.now();
        }
    }
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
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
