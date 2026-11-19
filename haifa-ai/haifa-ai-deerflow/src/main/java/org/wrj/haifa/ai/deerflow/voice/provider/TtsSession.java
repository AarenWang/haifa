package org.wrj.haifa.ai.deerflow.voice.provider;

import org.wrj.haifa.ai.deerflow.voice.domain.AudioChunk;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;
import org.wrj.haifa.ai.deerflow.voice.domain.TextChunk;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TtsSession extends AutoCloseable {
    Mono<Void> append(TextChunk chunk);
    Mono<Void> commit();
    Flux<AudioChunk> audio();
    default AudioFormat format() { return AudioFormat.DEFAULT_OUTPUT; }
    Mono<Void> cancel();

    @Override
    default void close() throws Exception {
        cancel().block();
    }
}
