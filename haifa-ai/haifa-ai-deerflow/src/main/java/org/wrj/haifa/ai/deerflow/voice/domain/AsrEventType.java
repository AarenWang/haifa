package org.wrj.haifa.ai.deerflow.voice.domain;

public enum AsrEventType {
    READY,
    SPEECH_STARTED,
    PARTIAL,
    FINAL,
    ERROR,
    COMPLETED
}
