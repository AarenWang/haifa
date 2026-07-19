package org.wrj.haifa.ai.deerflow.voice.provider.dashscope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.voice.VoiceProperties;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioChunk;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;
import org.wrj.haifa.ai.deerflow.voice.domain.TextChunk;
import org.wrj.haifa.ai.deerflow.voice.provider.StreamingTtsProvider;
import org.wrj.haifa.ai.deerflow.voice.provider.TtsSession;
import org.wrj.haifa.ai.deerflow.voice.provider.TtsStartOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DashScopeStreamingTtsProvider implements StreamingTtsProvider {

    private static final Logger log = LoggerFactory.getLogger(DashScopeStreamingTtsProvider.class);

    private final VoiceProperties.DashScope config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DashScopeStreamingTtsProvider(VoiceProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getTts().getProviders().getDashscope();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String id() { return "dashscope"; }

    @Override
    public boolean isAvailable() {
        return config.isEnabled()
                && config.getApiKey() != null && !config.getApiKey().isBlank()
                && config.getModel() != null && !config.getModel().isBlank()
                && config.getVoice() != null && !config.getVoice().isBlank();
    }

    @Override
    public String model() { return config.getModel(); }

    @Override
    public Mono<TtsSession> open(TtsStartOptions options) {
        if (!isAvailable()) {
            return Mono.error(new IllegalStateException(
                    "DashScope TTS is unavailable: enable it and configure api-key, workspace-id/endpoint, model, and voice"));
        }
        AudioFormat format = new AudioFormat("pcm_s16le", config.getSampleRateHz(), 1, 16, 100);
        String voice = options.voice() == null || options.voice().isBlank() || "default".equals(options.voice())
                ? config.getVoice()
                : options.voice();
        DashScopeTtsSession session = new DashScopeTtsSession(
                objectMapper, config.getModel(), voice, options.speed(), format);
        return Mono.fromFuture(httpClient.newWebSocketBuilder()
                        .header("Authorization", "Bearer " + config.getApiKey())
                        .connectTimeout(Duration.ofSeconds(10))
                        .buildAsync(URI.create(config.inferenceEndpoint()), session))
                .doOnNext(session::connected)
                .then(Mono.fromFuture(session.readyFuture()))
                .thenReturn(session);
    }

    static final class DashScopeTtsSession implements TtsSession, WebSocket.Listener {
        private final ObjectMapper objectMapper;
        private final String taskId = UUID.randomUUID().toString();
        private final String model;
        private final String voice;
        private final double speed;
        private final AudioFormat format;
        private final Sinks.Many<AudioChunk> audio = Sinks.many().unicast().onBackpressureBuffer();
        private final CompletableFuture<Void> ready = new CompletableFuture<>();
        private final AtomicLong sequence = new AtomicLong();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean committed = new AtomicBoolean();
        private final StringBuilder textBuffer = new StringBuilder();
        private final ByteArrayOutputStream binaryBuffer = new ByteArrayOutputStream();
        private volatile WebSocket webSocket;

        DashScopeTtsSession(ObjectMapper objectMapper, String model, String voice, double speed, AudioFormat format) {
            this.objectMapper = objectMapper;
            this.model = model;
            this.voice = voice;
            this.speed = speed;
            this.format = format;
        }

        CompletableFuture<Void> readyFuture() { return ready.orTimeout(10, java.util.concurrent.TimeUnit.SECONDS); }

        void connected(WebSocket webSocket) {
            this.webSocket = webSocket;
            send(Map.of(
                    "header", Map.of("action", "run-task", "task_id", taskId, "streaming", "duplex"),
                    "payload", Map.of(
                            "task_group", "audio",
                            "task", "tts",
                            "function", "SpeechSynthesizer",
                            "model", model,
                            "parameters", Map.of(
                                    "text_type", "PlainText",
                                    "voice", voice,
                                    "format", "pcm",
                                    "sample_rate", format.sampleRateHz(),
                                    "volume", 50,
                                    "rate", Math.max(0.5, Math.min(2.0, speed)),
                                    "pitch", 1.0,
                                    "enable_ssml", false
                            ),
                            "input", Map.of()
                    )
            )).subscribe(null, ready::completeExceptionally);
        }

        @Override
        public Mono<Void> append(TextChunk chunk) {
            if (cancelled.get() || committed.get() || chunk == null || chunk.text().isBlank()) return Mono.empty();
            return Mono.fromFuture(readyFuture()).then(send(Map.of(
                    "header", Map.of("action", "continue-task", "task_id", taskId, "streaming", "duplex"),
                    "payload", Map.of("input", Map.of("text", chunk.text()))
            )));
        }

        @Override
        public Mono<Void> commit() {
            if (cancelled.get() || !committed.compareAndSet(false, true)) return Mono.empty();
            return Mono.fromFuture(readyFuture()).then(send(Map.of(
                    "header", Map.of("action", "finish-task", "task_id", taskId, "streaming", "duplex"),
                    "payload", Map.of("input", Map.of())
            )));
        }

        @Override
        public Flux<AudioChunk> audio() { return audio.asFlux(); }

        @Override
        public AudioFormat format() { return format; }

        @Override
        public Mono<Void> cancel() {
            if (cancelled.compareAndSet(false, true)) {
                WebSocket socket = webSocket;
                if (socket != null) socket.abort();
                ready.completeExceptionally(new IllegalStateException("DashScope TTS session cancelled"));
                audio.tryEmitComplete();
            }
            return Mono.empty();
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                String json = textBuffer.toString();
                textBuffer.setLength(0);
                handleEvent(json);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            binaryBuffer.writeBytes(bytes);
            if (last) {
                byte[] frame = binaryBuffer.toByteArray();
                binaryBuffer.reset();
                if (!cancelled.get() && frame.length > 0) {
                    audio.tryEmitNext(new AudioChunk(sequence.incrementAndGet(), frame, format));
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (!ready.isDone()) ready.completeExceptionally(new IllegalStateException("DashScope TTS closed before ready"));
            audio.tryEmitComplete();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("DashScope TTS WebSocket error", error);
            ready.completeExceptionally(error);
            audio.tryEmitError(error);
        }

        private void handleEvent(String json) {
            try {
                JsonNode root = objectMapper.readTree(json);
                String event = root.path("header").path("event").asText();
                if ("task-started".equals(event)) {
                    ready.complete(null);
                } else if ("task-finished".equals(event)) {
                    audio.tryEmitComplete();
                } else if ("task-failed".equals(event)) {
                    String code = root.path("header").path("error_code").asText("unknown");
                    String message = root.path("header").path("error_message").asText("DashScope TTS task failed");
                    IllegalStateException error = new IllegalStateException(code + ": " + message);
                    log.error("DashScope TTS task failed event: {}", json, error);
                    ready.completeExceptionally(error);
                    audio.tryEmitError(error);
                }
            } catch (Exception ex) {
                log.error("Failed to parse DashScope TTS response event: {}", json, ex);
                ready.completeExceptionally(ex);
                audio.tryEmitError(new IllegalStateException("Invalid DashScope TTS event", ex));
            }
        }

        private Mono<Void> send(Map<String, ?> payload) {
            WebSocket socket = webSocket;
            if (socket == null) return Mono.error(new IllegalStateException("DashScope TTS WebSocket is not connected"));
            try {
                return Mono.fromFuture(socket.sendText(objectMapper.writeValueAsString(payload), true)).then();
            } catch (Exception ex) {
                return Mono.error(ex);
            }
        }
    }
}
