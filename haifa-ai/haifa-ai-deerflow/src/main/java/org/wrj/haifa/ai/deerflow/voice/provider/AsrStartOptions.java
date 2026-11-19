package org.wrj.haifa.ai.deerflow.voice.provider;

import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;
import java.util.Map;

public record AsrStartOptions(
        String language,
        AudioFormat format,
        String model,
        Map<String, Object> options
) {
    public AsrStartOptions {
        language = language == null ? "zh" : language;
        format = format == null ? AudioFormat.DEFAULT_INPUT : format;
        options = options == null ? Map.of() : options;
    }
}
