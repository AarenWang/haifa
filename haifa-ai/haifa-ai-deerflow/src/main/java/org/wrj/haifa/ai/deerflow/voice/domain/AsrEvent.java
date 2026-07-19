package org.wrj.haifa.ai.deerflow.voice.domain;

import java.util.Map;

public record AsrEvent(
        AsrEventType type,
        String text,
        boolean finalResult,
        String language,
        Map<String, Object> safeMetadata
) {
    public AsrEvent {
        safeMetadata = safeMetadata == null ? Map.of() : safeMetadata;
        text = text == null ? "" : text;
        language = language == null ? "zh" : language;
    }

    public static AsrEvent partial(String text) {
        return new AsrEvent(AsrEventType.PARTIAL, text, false, "zh", Map.of());
    }

    public static AsrEvent finalEvent(String text) {
        return new AsrEvent(AsrEventType.FINAL, text, true, "zh", Map.of());
    }

    public static AsrEvent error(String message) {
        return new AsrEvent(AsrEventType.ERROR, message, false, "zh", Map.of("error", message));
    }
}
