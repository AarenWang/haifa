package org.wrj.haifa.ai.deerflow.voice.provider.volcengine;

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
import reactor.core.Disposable;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Component
public class VolcengineStreamingTtsProvider implements StreamingTtsProvider {

    private static final Logger log = LoggerFactory.getLogger(VolcengineStreamingTtsProvider.class);

    private final VoiceProperties.VolcengineTts config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public VolcengineStreamingTtsProvider(VoiceProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getTts().getProviders().getVolcengine();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String id() { return "volcengine"; }

    @Override
    public boolean isAvailable() {
        return config.isEnabled()
                && notBlank(config.getAppId())
                && notBlank(config.getAccessToken())
                && notBlank(config.getEndpoint())
                && notBlank(config.getVoice());
    }

    @Override
    public String model() { return config.getVoice(); }

    @Override
    public Mono<TtsSession> open(TtsStartOptions options) {
        if (!isAvailable()) {
            return Mono.error(new IllegalStateException(
                    "Volcengine TTS is unavailable: enable it and configure app-id, access-token, endpoint, and voice"));
        }
        String voice = options.voice() == null || options.voice().isBlank() || "default".equals(options.voice())
                ? config.getVoice()
                : options.voice();
        AudioFormat format = new AudioFormat("pcm_s16le", config.getSampleRateHz(), 1, 16, 100);
        return Mono.just(new VolcengineTtsSession(httpClient, objectMapper, config, voice, options.speed(), format));
    }

    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }

    static final class VolcengineTtsSession implements TtsSession {
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;
        private final VoiceProperties.VolcengineTts config;
        private final String voice;
        private final double speed;
        private final AudioFormat format;
        private final Sinks.Many<TextChunk> text = Sinks.many().unicast().onBackpressureBuffer();
        private final Sinks.Many<AudioChunk> audio = Sinks.many().unicast().onBackpressureBuffer();
        private final AtomicLong sequence = new AtomicLong();
        private final AtomicBoolean committed = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final Set<WebSocket> activeSockets = ConcurrentHashMap.newKeySet();
        private final Disposable worker;

        VolcengineTtsSession(HttpClient httpClient, ObjectMapper objectMapper,
                             VoiceProperties.VolcengineTts config, String voice,
                             double speed, AudioFormat format) {
            this.httpClient = httpClient;
            this.objectMapper = objectMapper;
            this.config = config;
            this.voice = voice;
            this.speed = speed;
            this.format = format;
            this.worker = text.asFlux()
                    .concatMap(this::synthesize, 1)
                    .subscribe(null, audio::tryEmitError, audio::tryEmitComplete);
        }

        @Override
        public Mono<Void> append(TextChunk chunk) {
            if (cancelled.get() || committed.get() || chunk == null || chunk.text().isBlank()) return Mono.empty();
            Sinks.EmitResult result = text.tryEmitNext(chunk);
            return result.isFailure()
                    ? Mono.error(new IllegalStateException("Unable to queue Volcengine TTS text: " + result))
                    : Mono.empty();
        }

        @Override
        public Mono<Void> commit() {
            if (!cancelled.get() && committed.compareAndSet(false, true)) text.tryEmitComplete();
            return Mono.empty();
        }

        @Override
        public Flux<AudioChunk> audio() { return audio.asFlux(); }

        @Override
        public AudioFormat format() { return format; }

        @Override
        public Mono<Void> cancel() {
            if (cancelled.compareAndSet(false, true)) {
                text.tryEmitComplete();
                worker.dispose();
                activeSockets.forEach(WebSocket::abort);
                activeSockets.clear();
                audio.tryEmitComplete();
            }
            return Mono.empty();
        }

        private Mono<Void> synthesize(TextChunk chunk) {
            if (cancelled.get()) return Mono.empty();
            CompletableFuture<Void> completion = new CompletableFuture<>();
            try {
                byte[] request = encodeRequest(chunk.text());
                VolcengineListener listener = new VolcengineListener(format, sequence, audio, completion, activeSockets);
                return Mono.fromFuture(httpClient.newWebSocketBuilder()
                                .header("Authorization", "Bearer;" + config.getAccessToken())
                                .connectTimeout(Duration.ofSeconds(10))
                                .buildAsync(URI.create(config.getEndpoint()), listener))
                        .doOnNext(socket -> {
                            activeSockets.add(socket);
                            listener.connected(socket);
                            socket.sendBinary(ByteBuffer.wrap(request), true)
                                    .exceptionally(error -> { completion.completeExceptionally(error); return null; });
                        })
                        .then(Mono.fromFuture(completion.orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)));
            } catch (Exception ex) {
                return Mono.error(ex);
            }
        }

        private byte[] encodeRequest(String content) throws Exception {
            Map<String, Object> request = Map.of(
                    "app", Map.of(
                            "appid", config.getAppId(),
                            "token", config.getAccessToken(),
                            "cluster", config.getCluster()),
                    "user", Map.of("uid", "haifa-deerflow"),
                    "audio", Map.of(
                            "voice_type", voice,
                            "encoding", "pcm",
                            "rate", format.sampleRateHz(),
                            "speed_ratio", Math.max(0.5, Math.min(2.0, speed))),
                    "request", Map.of(
                            "reqid", UUID.randomUUID().toString(),
                            "text", content,
                            "operation", "submit")
            );
            byte[] compressed = gzip(objectMapper.writeValueAsBytes(request));
            ByteBuffer frame = ByteBuffer.allocate(8 + compressed.length);
            frame.put((byte) 0x11);
            frame.put((byte) 0x10);
            frame.put((byte) 0x11);
            frame.put((byte) 0x00);
            frame.putInt(compressed.length);
            frame.put(compressed);
            return frame.array();
        }

        private static byte[] gzip(byte[] source) throws Exception {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(source);
            }
            return output.toByteArray();
        }
    }

    static final class VolcengineListener implements WebSocket.Listener {
        private final AudioFormat format;
        private final AtomicLong sequence;
        private final Sinks.Many<AudioChunk> audio;
        private final CompletableFuture<Void> completion;
        private final Set<WebSocket> activeSockets;
        private final ByteArrayOutputStream fragments = new ByteArrayOutputStream();
        private volatile WebSocket socket;

        VolcengineListener(AudioFormat format, AtomicLong sequence, Sinks.Many<AudioChunk> audio,
                           CompletableFuture<Void> completion, Set<WebSocket> activeSockets) {
            this.format = format;
            this.sequence = sequence;
            this.audio = audio;
            this.completion = completion;
            this.activeSockets = activeSockets;
        }

        void connected(WebSocket socket) { this.socket = socket; }

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] part = new byte[data.remaining()];
            data.get(part);
            fragments.writeBytes(part);
            if (last) {
                byte[] frame = fragments.toByteArray();
                fragments.reset();
                try {
                    parseFrame(frame);
                } catch (Exception ex) {
                    completion.completeExceptionally(ex);
                    webSocket.abort();
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            activeSockets.remove(webSocket);
            if (!completion.isDone()) completion.complete(null);
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("Volcengine TTS WebSocket error", error);
            activeSockets.remove(webSocket);
            completion.completeExceptionally(error);
        }

        private void parseFrame(byte[] frame) throws Exception {
            if (frame.length < 4) throw new IllegalArgumentException("Invalid Volcengine TTS response frame");
            ByteBuffer buffer = ByteBuffer.wrap(frame);
            int headerSize = (buffer.get(0) & 0x0f) * 4;
            int messageType = (buffer.get(1) >> 4) & 0x0f;
            int flags = buffer.get(1) & 0x0f;
            int compression = buffer.get(2) & 0x0f;
            if (headerSize < 4 || headerSize > buffer.remaining()) {
                throw new IllegalArgumentException("Invalid Volcengine TTS header size: " + headerSize);
            }
            buffer.position(headerSize);
            if (messageType == 0x0b) {
                int providerSequence = flags == 0 ? 0 : buffer.getInt();
                int payloadSize = buffer.getInt();
                if (payloadSize < 0 || payloadSize > buffer.remaining()) {
                    throw new IllegalArgumentException("Invalid Volcengine TTS audio payload size: " + payloadSize);
                }
                byte[] payload = new byte[payloadSize];
                buffer.get(payload);
                if (payload.length > 0) {
                    audio.tryEmitNext(new AudioChunk(sequence.incrementAndGet(), payload, format));
                }
                if (providerSequence < 0 || (flags & 0x02) != 0) finish();
            } else if (messageType == 0x0f) {
                int code = buffer.getInt();
                int payloadSize = buffer.getInt();
                byte[] payload = new byte[Math.min(Math.max(payloadSize, 0), buffer.remaining())];
                buffer.get(payload);
                if (compression == 1 && payload.length > 0) payload = gunzip(payload);
                throw new IllegalStateException("Volcengine TTS error " + code + ": "
                        + new String(payload, java.nio.charset.StandardCharsets.UTF_8));
            }
        }

        private void finish() {
            WebSocket current = socket;
            if (current != null) {
                activeSockets.remove(current);
                current.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            }
            completion.complete(null);
        }

        private static byte[] gunzip(byte[] source) throws Exception {
            try (GZIPInputStream input = new GZIPInputStream(new java.io.ByteArrayInputStream(source))) {
                return input.readAllBytes();
            }
        }
    }
}
