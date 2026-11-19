package org.wrj.haifa.ai.deerflow.voice;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.voice.provider.*;
import org.wrj.haifa.ai.deerflow.voice.provider.fake.FakeStreamingAsrProvider;
import org.wrj.haifa.ai.deerflow.voice.provider.fake.FakeStreamingTtsProvider;
import org.wrj.haifa.ai.deerflow.voice.VoiceProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VoiceProviderResolverTest {

    @Test
    public void testResolveAsrAndTtsFallbackToFake() {
        StreamingAsrProviderRegistry asrReg = new StreamingAsrProviderRegistry(List.of(new FakeStreamingAsrProvider()));
        StreamingTtsProviderRegistry ttsReg = new StreamingTtsProviderRegistry(List.of(new FakeStreamingTtsProvider()));
        VoiceProviderResolver resolver = new VoiceProviderResolver(asrReg, ttsReg, new VoiceProperties());

        StreamingAsrProvider asr = resolver.resolveAsr(null, null);
        assertEquals("fake", asr.id());

        StreamingTtsProvider tts = resolver.resolveTts(null, null);
        assertEquals("fake", tts.id());
    }

    @Test
    public void testDoesNotSilentlyFallbackFromUnavailableExplicitProvider() {
        StreamingAsrProvider unavailable = new StreamingAsrProvider() {
            @Override public String id() { return "cloud"; }
            @Override public boolean isAvailable() { return false; }
            @Override public reactor.core.publisher.Mono<AsrSession> open(AsrStartOptions options) {
                return reactor.core.publisher.Mono.error(new IllegalStateException("disabled"));
            }
        };
        StreamingAsrProviderRegistry asrReg = new StreamingAsrProviderRegistry(
                List.of(new FakeStreamingAsrProvider(), unavailable));
        StreamingTtsProviderRegistry ttsReg = new StreamingTtsProviderRegistry(
                List.of(new FakeStreamingTtsProvider()));
        VoiceProviderResolver resolver = new VoiceProviderResolver(asrReg, ttsReg, new VoiceProperties());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> resolver.resolveAsr("cloud", null));
        assertTrue(error.getMessage().contains("not enabled or configured"));
        assertTrue(error.getMessage().contains("cloud"));
    }
}
