package org.wrj.haifa.ai.deerflow.voice.provider.fake;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioChunk;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;
import org.wrj.haifa.ai.deerflow.voice.domain.TextChunk;
import org.wrj.haifa.ai.deerflow.voice.provider.StreamingTtsProvider;
import org.wrj.haifa.ai.deerflow.voice.provider.TtsSession;
import org.wrj.haifa.ai.deerflow.voice.provider.TtsStartOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FakeStreamingTtsProvider implements StreamingTtsProvider {

    @Override
    public String id() {
        return "fake";
    }

    @Override
    public Mono<TtsSession> open(TtsStartOptions options) {
        return Mono.just(new FakeTtsSession(options.format()));
    }

    public static class FakeTtsSession implements TtsSession {
        private final AudioFormat format;
        private final Sinks.Many<AudioChunk> audioSink = Sinks.many().multicast().onBackpressureBuffer();
        private final AtomicLong seqCounter = new AtomicLong(1);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        public FakeTtsSession(AudioFormat format) {
            this.format = format == null ? AudioFormat.DEFAULT_OUTPUT : format;
        }

        @Override
        public Mono<Void> append(TextChunk chunk) {
            if (cancelled.get() || chunk.text().isBlank()) return Mono.empty();
            // Generate 100ms fake PCM audio chunk (1600 samples = 3200 bytes for 16kHz s16le, or 4800 bytes for 24kHz)
            int sampleRate = format.sampleRateHz();
            int bytesPerFrame = (sampleRate * 2 * format.chunkDurationMs()) / 1000;
            byte[] fakePcm = new byte[bytesPerFrame];
            long seq = seqCounter.getAndIncrement();
            audioSink.tryEmitNext(new AudioChunk(seq, fakePcm, format));
            return Mono.empty();
        }

        @Override
        public Mono<Void> commit() {
            if (cancelled.get()) return Mono.empty();
            audioSink.tryEmitComplete();
            return Mono.empty();
        }

        @Override
        public Flux<AudioChunk> audio() {
            return audioSink.asFlux();
        }

        @Override
        public Mono<Void> cancel() {
            cancelled.set(true);
            audioSink.tryEmitComplete();
            return Mono.empty();
        }
    }
}
