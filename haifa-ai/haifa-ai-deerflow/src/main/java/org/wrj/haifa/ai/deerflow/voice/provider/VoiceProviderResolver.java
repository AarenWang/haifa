package org.wrj.haifa.ai.deerflow.voice.provider;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.voice.VoiceProperties;

@Component
public class VoiceProviderResolver {
    private final StreamingAsrProviderRegistry asrRegistry;
    private final StreamingTtsProviderRegistry ttsRegistry;

    private final String defaultAsrProvider;
    private final String defaultTtsProvider;

    public VoiceProviderResolver(StreamingAsrProviderRegistry asrRegistry,
                                 StreamingTtsProviderRegistry ttsRegistry,
                                 VoiceProperties properties) {
        this.asrRegistry = asrRegistry;
        this.ttsRegistry = ttsRegistry;
        this.defaultAsrProvider = properties.getAsr().getDefaultProvider();
        this.defaultTtsProvider = properties.getTts().getDefaultProvider();
    }

    public StreamingAsrProvider resolveAsr(String override, String sessionPref) {
        if (override != null && !override.isBlank()) {
            return requireAvailableAsr(override, "requested ASR override");
        }
        if (sessionPref != null && !sessionPref.isBlank()) {
            return requireAvailableAsr(sessionPref, "voice session ASR provider");
        }
        return requireAvailableAsr(defaultAsrProvider, "default ASR provider");
    }

    public StreamingTtsProvider resolveTts(String override, String sessionPref) {
        if (override != null && !override.isBlank()) {
            return requireAvailableTts(override, "requested TTS override");
        }
        if (sessionPref != null && !sessionPref.isBlank()) {
            return requireAvailableTts(sessionPref, "voice session TTS provider");
        }
        return requireAvailableTts(defaultTtsProvider, "default TTS provider");
    }

    private StreamingAsrProvider requireAvailableAsr(String id, String source) {
        StreamingAsrProvider provider = asrRegistry.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + source + ": " + id));
        if (!provider.isAvailable()) {
            throw new IllegalStateException(source + " is not enabled or configured: " + id);
        }
        return provider;
    }

    private StreamingTtsProvider requireAvailableTts(String id, String source) {
        StreamingTtsProvider provider = ttsRegistry.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + source + ": " + id));
        if (!provider.isAvailable()) {
            throw new IllegalStateException(source + " is not enabled or configured: " + id);
        }
        return provider;
    }
}
