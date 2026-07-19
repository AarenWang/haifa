package org.wrj.haifa.ai.deerflow.voice.provider.fake;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.voice.domain.AsrEvent;
import org.wrj.haifa.ai.deerflow.voice.provider.AsrSession;
import org.wrj.haifa.ai.deerflow.voice.provider.AsrStartOptions;
import org.wrj.haifa.ai.deerflow.voice.provider.StreamingAsrProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class FakeStreamingAsrProvider implements StreamingAsrProvider {

    @Override
    public String id() {
        return "fake";
    }

    @Override
    public Mono<AsrSession> open(AsrStartOptions options) {
        return Mono.just(new FakeAsrSession());
    }

    public static class FakeAsrSession implements AsrSession {
        private final Sinks.Many<AsrEvent> eventSink = Sinks.many().multicast().onBackpressureBuffer();
        private final AtomicBoolean committed = new AtomicBoolean(false);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        @Override
        public Mono<Void> append(byte[] pcmData) {
            if (cancelled.get()) return Mono.empty();
            eventSink.tryEmitNext(AsrEvent.partial("正在识别中..."));
            return Mono.empty();
        }

        @Override
        public Mono<Void> commit() {
            if (cancelled.get() || committed.getAndSet(true)) return Mono.empty();
            eventSink.tryEmitNext(AsrEvent.partial("你好，请帮我简要介绍一下 DeerFlow 架构。"));
            eventSink.tryEmitNext(AsrEvent.finalEvent("你好，请帮我简要介绍一下 DeerFlow 架构。"));
            eventSink.tryEmitComplete();
            return Mono.empty();
        }

        @Override
        public Flux<AsrEvent> events() {
            return eventSink.asFlux();
        }

        @Override
        public Mono<Void> cancel() {
            cancelled.set(true);
            eventSink.tryEmitComplete();
            return Mono.empty();
        }
    }
}
