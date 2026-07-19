package org.wrj.haifa.ai.deerflow.voice.provider.dashscope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.voice.VoiceProperties;
import org.wrj.haifa.ai.deerflow.voice.domain.AsrEvent;
import org.wrj.haifa.ai.deerflow.voice.domain.AsrEventType;
import org.wrj.haifa.ai.deerflow.voice.provider.AsrSession;
import org.wrj.haifa.ai.deerflow.voice.provider.AsrStartOptions;
import org.wrj.haifa.ai.deerflow.voice.provider.StreamingAsrProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DashScopeStreamingAsrProvider implements StreamingAsrProvider {

    private static final Logger log = LoggerFactory.getLogger(DashScopeStreamingAsrProvider.class);

    private final VoiceProperties.DashScope config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DashScopeStreamingAsrProvider(VoiceProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getAsr().getProviders().getDashscope();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String id() { return "dashscope"; }

    @Override
    public boolean isAvailable() {
        return config.isEnabled()
                && config.getApiKey() != null && !config.getApiKey().isBlank()
                && config.getModel() != null && !config.getModel().isBlank();
    }

    @Override
    public String model() { return config.getModel(); }

    @Override
    public Mono<AsrSession> open(AsrStartOptions options) {
        if (!isAvailable()) {
            return Mono.error(new IllegalStateException(
                    "DashScope ASR is unavailable: enable it and configure api-key, workspace-id/endpoint, and model"));
        }
        String separator = config.realtimeEndpoint().contains("?") ? "&" : "?";
        URI uri = URI.create(config.realtimeEndpoint() + separator + "model="
                + URLEncoder.encode(config.getModel(), StandardCharsets.UTF_8));
        DashScopeAsrSession session = new DashScopeAsrSession(options, objectMapper);
        var builder = httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer " + config.getApiKey())
                .connectTimeout(Duration.ofSeconds(10));
        if (config.getWorkspaceId() != null && !config.getWorkspaceId().isBlank()) {
            builder.header("X-DashScope-WorkSpace", config.getWorkspaceId());
        }
        return Mono.fromFuture(builder.buildAsync(uri, session))
                .doOnNext(session::connected)
                .then(Mono.fromFuture(session.readyFuture()))
                .thenReturn(session);
    }

    static final class DashScopeAsrSession implements AsrSession, WebSocket.Listener {
        private final AsrStartOptions options;
        private final ObjectMapper objectMapper;
        private final Sinks.Many<AsrEvent> events = Sinks.many().replay().limit(32);
        private final StringBuilder textBuffer = new StringBuilder();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean committed = new AtomicBoolean();
        private final CompletableFuture<Void> ready = new CompletableFuture<>();
        private volatile WebSocket webSocket;

        DashScopeAsrSession(AsrStartOptions options, ObjectMapper objectMapper) {
            this.options = options;
            this.objectMapper = objectMapper;
        }

        void connected(WebSocket webSocket) {
            this.webSocket = webSocket;
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("input_audio_format", "pcm");
            session.put("sample_rate", options.format().sampleRateHz());
            session.put("input_audio_transcription", Map.of("language", normalizeLanguage(options.language())));
            session.put("turn_detection", null);
            send(Map.of(
                    "event_id", eventId(),
                    "type", "session.update",
                    "session", session
            )).subscribe(null, error -> {
                ready.completeExceptionally(error);
                events.tryEmitError(error);
            });
        }

        CompletableFuture<Void> readyFuture() { return ready.orTimeout(10, TimeUnit.SECONDS); }

        @Override
        public Mono<Void> append(byte[] pcmData) {
            if (cancelled.get()) return Mono.empty();
            if (pcmData == null || pcmData.length == 0) return Mono.empty();
            return send(Map.of(
                    "event_id", eventId(),
                    "type", "input_audio_buffer.append",
                    "audio", Base64.getEncoder().encodeToString(pcmData)
            ));
        }

        @Override
        public Mono<Void> commit() {
            if (cancelled.get() || !committed.compareAndSet(false, true)) return Mono.empty();
            return send(Map.of("event_id", eventId(), "type", "input_audio_buffer.commit"))
                    .then(send(Map.of("event_id", eventId(), "type", "session.finish")));
        }

        @Override
        public Flux<AsrEvent> events() { return events.asFlux(); }

        @Override
        public Mono<Void> cancel() {
            if (cancelled.compareAndSet(false, true)) {
                WebSocket socket = webSocket;
                if (socket != null) socket.abort();
                events.tryEmitComplete();
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
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (!ready.isDone()) ready.completeExceptionally(
                    new IllegalStateException("DashScope ASR closed before ready"));
            events.tryEmitComplete();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("DashScope ASR WebSocket error", error);
            ready.completeExceptionally(error);
            events.tryEmitError(error);
        }

        private void handleEvent(String json) {
            try {
                JsonNode root = objectMapper.readTree(json);
                String type = root.path("type").asText();
                switch (type) {
                    case "session.updated" -> {
                        ready.complete(null);
                        events.tryEmitNext(new AsrEvent(
                                AsrEventType.READY, "", false, options.language(), Map.of()));
                    }
                    case "input_audio_buffer.speech_started" -> events.tryEmitNext(
                            new AsrEvent(AsrEventType.SPEECH_STARTED, "", false, options.language(), Map.of()));
                    case "conversation.item.input_audio_transcription.text" -> {
                        String text = root.path("text").asText("") + root.path("stash").asText("");
                        events.tryEmitNext(AsrEvent.partial(text));
                    }
                    case "conversation.item.input_audio_transcription.completed" -> {
                        String text = firstText(root, "transcript", "text");
                        events.tryEmitNext(AsrEvent.finalEvent(text));
                    }
                    case "session.finished" -> events.tryEmitComplete();
                    case "error" -> {
                        IllegalStateException error = new IllegalStateException(providerError(root));
                        log.error("DashScope ASR server returned error event: {}", json, error);
                        ready.completeExceptionally(error);
                        events.tryEmitError(error);
                    }
                    default -> { }
                }
            } catch (Exception ex) {
                log.error("Failed to parse DashScope ASR response event: {}", json, ex);
                ready.completeExceptionally(ex);
                events.tryEmitError(new IllegalStateException("Invalid DashScope ASR event", ex));
            }
        }

        private Mono<Void> send(Map<String, ?> payload) {
            WebSocket socket = webSocket;
            if (socket == null) return Mono.error(new IllegalStateException("DashScope ASR WebSocket is not connected"));
            try {
                return Mono.fromFuture(socket.sendText(objectMapper.writeValueAsString(payload), true)).then();
            } catch (Exception ex) {
                return Mono.error(ex);
            }
        }

        private static String normalizeLanguage(String value) {
            if (value == null || value.isBlank()) return "zh";
            int separator = value.indexOf('-');
            return (separator > 0 ? value.substring(0, separator) : value).toLowerCase();
        }

        private static String firstText(JsonNode root, String... fields) {
            for (String field : fields) {
                String value = root.path(field).asText("");
                if (!value.isBlank()) return value;
            }
            return "";
        }

        private static String providerError(JsonNode root) {
            JsonNode error = root.path("error");
            String code = error.path("code").asText("unknown");
            String message = error.path("message").asText("DashScope ASR request failed");
            return code + ": " + message;
        }

        private static String eventId() { return "event_" + UUID.randomUUID(); }
    }
}
