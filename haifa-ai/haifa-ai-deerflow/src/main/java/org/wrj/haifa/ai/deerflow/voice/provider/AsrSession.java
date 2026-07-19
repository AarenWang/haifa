package org.wrj.haifa.ai.deerflow.voice.provider;

import org.wrj.haifa.ai.deerflow.voice.domain.AsrEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AsrSession extends AutoCloseable {
    Mono<Void> append(byte[] pcmData);
    Mono<Void> commit();
    Flux<AsrEvent> events();
    Mono<Void> cancel();

    @Override
    default void close() throws Exception {
        cancel().block();
    }
}
