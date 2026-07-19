package org.wrj.haifa.ai.deerflow.voice.provider;

import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;
import java.util.Map;

public record TtsStartOptions(
        String voice,
        double speed,
        String model,
        AudioFormat format,
        Map<String, Object> options
) {
    public TtsStartOptions {
        voice = voice == null ? "default" : voice;
        speed = speed <= 0 ? 1.0 : speed;
        format = format == null ? AudioFormat.DEFAULT_OUTPUT : format;
        options = options == null ? Map.of() : options;
    }
}
