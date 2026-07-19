package org.wrj.haifa.ai.deerflow.voice.provider;

import reactor.core.publisher.Mono;

public interface StreamingAsrProvider {
    String id();
    default boolean isAvailable() { return true; }
    default String model() { return ""; }
    Mono<AsrSession> open(AsrStartOptions options);
}
