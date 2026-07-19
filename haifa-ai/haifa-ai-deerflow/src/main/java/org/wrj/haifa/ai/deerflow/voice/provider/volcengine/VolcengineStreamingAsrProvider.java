package org.wrj.haifa.ai.deerflow.voice.provider.volcengine;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class VolcengineStreamingAsrProvider implements StreamingAsrProvider {

    private static final Logger log = LoggerFactory.getLogger(VolcengineStreamingAsrProvider.class);

    private final VoiceProperties.VolcengineAsr config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public VolcengineStreamingAsrProvider(VoiceProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getAsr().getProviders().getVolcengine();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String id() { return "volcengine"; }

    @Override
    public boolean isAvailable() {
        return config.isEnabled()
                && config.getApiKey() != null && !config.getApiKey().isBlank()
                && config.getEndpoint() != null && !config.getEndpoint().isBlank()
                && config.getModel() != null && !config.getModel().isBlank();
    }

    @Override
    public String model() { return config.getModel(); }

    @Override
    public Mono<AsrSession> open(AsrStartOptions options) {
        if (!isAvailable()) {
            return Mono.error(new IllegalStateException(
                    "Volcengine ASR is unavailable: enable it and configure api-key, endpoint, and model"));
        }
        String separator = config.getEndpoint().contains("?") ? "&" : "?";
        URI uri = URI.create(config.getEndpoint() + separator + "model="
                + URLEncoder.encode(config.getModel(), StandardCharsets.UTF_8));
        VolcengineRealtimeAsrSession session = new VolcengineRealtimeAsrSession(options, objectMapper);
        WebSocket.Builder builder = httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer " + config.getApiKey())
                .connectTimeout(Duration.ofSeconds(10));
        if (config.getResourceId() != null && !config.getResourceId().isBlank()) {
            builder.header("X-Api-Resource-Id", config.getResourceId());
        }
        return Mono.fromFuture(builder.buildAsync(uri, session))
                .doOnNext(session::connected)
                .then(Mono.fromFuture(session.readyFuture()))
                .thenReturn(session);
    }

    static final class VolcengineRealtimeAsrSession implements AsrSession, WebSocket.Listener {
        private final AsrStartOptions options;
        private final ObjectMapper objectMapper;
        private final Sinks.Many<AsrEvent> events = Sinks.many().replay().limit(32);
        private final StringBuilder buffer = new StringBuilder();
        private final AtomicBoolean committed = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final CompletableFuture<Void> ready = new CompletableFuture<>();
        private volatile WebSocket webSocket;

        VolcengineRealtimeAsrSession(AsrStartOptions options, ObjectMapper objectMapper) {
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
            send(Map.of("event_id", eventId(), "type", "transcription_session.update", "session", session))
                    .subscribe(null, error -> {
                        ready.completeExceptionally(error);
                        events.tryEmitError(error);
                    });
        }

        CompletableFuture<Void> readyFuture() { return ready.orTimeout(10, TimeUnit.SECONDS); }

        @Override
        public Mono<Void> append(byte[] pcmData) {
            if (cancelled.get() || pcmData == null || pcmData.length == 0) return Mono.empty();
            return send(Map.of(
                    "event_id", eventId(),
                    "type", "input_audio_buffer.append",
                    "audio", Base64.getEncoder().encodeToString(pcmData)
            ));
        }

        @Override
        public Mono<Void> commit() {
            if (cancelled.get() || !committed.compareAndSet(false, true)) return Mono.empty();
            return send(Map.of("event_id", eventId(), "type", "input_audio_buffer.commit"));
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
            buffer.append(data);
            if (last) {
                String json = buffer.toString();
                buffer.setLength(0);
                handleEvent(json);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (!ready.isDone()) ready.completeExceptionally(
                    new IllegalStateException("Volcengine ASR closed before ready"));
            events.tryEmitComplete();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("Volcengine ASR WebSocket error", error);
            ready.completeExceptionally(error);
            events.tryEmitError(error);
        }

        private void handleEvent(String json) {
            try {
                JsonNode root = objectMapper.readTree(json);
                String type = root.path("type").asText();
                switch (type) {
                    case "transcription_session.updated" -> {
                        ready.complete(null);
                        events.tryEmitNext(new AsrEvent(
                                AsrEventType.READY, "", false, options.language(), Map.of()));
                    }
                    case "input_audio_buffer.speech_started" -> events.tryEmitNext(
                            new AsrEvent(AsrEventType.SPEECH_STARTED, "", false, options.language(), Map.of()));
                    case "conversation.item.input_audio_transcription.result" -> events.tryEmitNext(
                            AsrEvent.partial(firstText(root, "text", "transcript")));
                    case "conversation.item.input_audio_transcription.completed" -> events.tryEmitNext(
                            AsrEvent.finalEvent(firstText(root, "transcript", "text")));
                    case "error" -> {
                        IllegalStateException error = new IllegalStateException(providerError(root));
                        log.error("Volcengine ASR server returned error event: {}", json, error);
                        ready.completeExceptionally(error);
                        events.tryEmitError(error);
                    }
                    default -> { }
                }
            } catch (Exception ex) {
                log.error("Failed to parse Volcengine ASR response event: {}", json, ex);
                ready.completeExceptionally(ex);
                events.tryEmitError(new IllegalStateException("Invalid Volcengine ASR event", ex));
            }
        }

        private Mono<Void> send(Map<String, ?> payload) {
            WebSocket socket = webSocket;
            if (socket == null) return Mono.error(new IllegalStateException("Volcengine ASR WebSocket is not connected"));
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
            return error.path("code").asText("unknown") + ": "
                    + error.path("message").asText("Volcengine ASR request failed");
        }

        private static String eventId() { return "event_" + UUID.randomUUID(); }
    }
}
