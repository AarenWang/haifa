package org.wrj.haifa.ai.deerflow.voice.domain;

public enum VoiceTurnStatus {
    CREATED,
    LISTENING,
    TRANSCRIBING,
    WAITING_AGENT,
    SPEAKING,
    COMPLETED,
    CANCELLED,
    ERROR
}
