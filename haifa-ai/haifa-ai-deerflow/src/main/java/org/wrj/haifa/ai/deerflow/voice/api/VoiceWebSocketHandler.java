package org.wrj.haifa.ai.deerflow.voice.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import org.wrj.haifa.ai.deerflow.voice.VoiceProperties;
import org.wrj.haifa.ai.deerflow.voice.application.VoiceProtocol;
import org.wrj.haifa.ai.deerflow.voice.application.VoiceSessionService;
import org.wrj.haifa.ai.deerflow.voice.application.VoiceTurnCoordinator;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioChunk;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;
import org.wrj.haifa.ai.deerflow.voice.domain.VoiceSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class VoiceWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(VoiceWebSocketHandler.class);
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final VoiceSessionService sessionService;
    private final VoiceTurnCoordinator turnCoordinator;
    private final VoiceProperties properties;

    public VoiceWebSocketHandler(VoiceSessionService sessionService,
                                 VoiceTurnCoordinator turnCoordinator,
                                 VoiceProperties properties) {
        this.sessionService = sessionService;
        this.turnCoordinator = turnCoordinator;
        this.properties = properties;
    }

    @Override
    public Mono<Void> handle(WebSocketSession wsSession) {
        String wsSessionId = wsSession.getId();
        String userId = resolveUserId(wsSession);
        log.info("Voice WebSocket connected: session={}, user={}", wsSessionId, userId);
        SessionContext context = new SessionContext(userId);

        Mono<Void> inbound = wsSession.receive()
                .timeout(properties.getWebsocket().getIdleTimeout())
                .concatMap(message -> switch (message.getType()) {
                    case TEXT -> handleTextMessage(context, message.getPayloadAsText());
                    case BINARY -> {
                        int readable = message.getPayload().readableByteCount();
                        if (readable > properties.getWebsocket().getMaxFrameBytes()) {
                            yield protocolFailure(context, "VOICE_FRAME_TOO_LARGE", "Audio frame exceeds configured limit");
                        }
                        byte[] data = new byte[readable];
                        message.getPayload().read(data);
                        yield handleBinaryAudioFrame(context, data);
                    }
                    default -> Mono.empty();
                })
                .onErrorResume(error -> {
                    log.error("Voice WebSocket inbound stream error for user {}", context.userId, error);
                    return protocolFailure(context, "VOICE_CONNECTION_ERROR", safeMessage(error))
                            .doFinally(signal -> context.outboundSink.tryEmitComplete());
                })
                .then();

        Flux<WebSocketMessage> outbound = context.outboundSink.asFlux().map(payload -> {
            if (payload instanceof String json) return wsSession.textMessage(json);
            if (payload instanceof byte[] bytes) return wsSession.binaryMessage(factory -> factory.wrap(bytes));
            throw new IllegalArgumentException("Unsupported voice outbound payload");
        });

        return wsSession.send(outbound).and(inbound).doFinally(signal -> {
            log.info("Voice WebSocket disconnected: session={}, user={}", wsSessionId, userId);
            context.disposeTurnStreams();
            VoiceTurnCoordinator.ActiveTurn turn = context.activeTurn;
            if (turn != null && !turn.isTerminal()) turn.cancel("DISCONNECTED").subscribe();
            if (context.voiceSession != null) sessionService.closeSession(context.voiceSession.getVoiceSessionId());
        });
    }

    private Mono<Void> handleTextMessage(SessionContext context, String text) {
        try {
            VoiceProtocol.ControlMessage message = VoiceProtocol.decodeControlJson(text);
            String type = message.type() == null ? "" : message.type().trim().toLowerCase();
            return switch (type) {
                case "session.create" -> createSession(context, message);
                case "turn.start" -> startTurn(context, message);
                case "turn.commit" -> withMatchingTurn(context, message, VoiceTurnCoordinator.ActiveTurn::commit);
                case "turn.cancel" -> withMatchingTurn(context, message,
                        turn -> turn.cancel(message.reason() == null ? "USER_CANCELLED" : message.reason()));
                case "session.close" -> closeSession(context);
                default -> protocolFailure(context, "VOICE_PROTOCOL_ERROR", "Unknown control message type");
            };
        } catch (IllegalArgumentException error) {
            log.warn("Voice protocol validation warning: {}", safeMessage(error), error);
            return protocolFailure(context, "VOICE_PROTOCOL_ERROR", safeMessage(error));
        } catch (Exception error) {
            log.error("Voice control message execution failed: {}", safeMessage(error), error);
            return protocolFailure(context, "VOICE_INTERNAL_ERROR", "Voice request could not be processed");
        }
    }

    private Mono<Void> createSession(SessionContext context, VoiceProtocol.ControlMessage message) {
        if (context.voiceSession != null) {
            VoiceSession session = context.voiceSession;
            context.sendControl(new VoiceProtocol.ControlMessage(
                    "session.ready", session.getVoiceSessionId(), null, session.getThreadId(),
                    null, null, null, null, null,
                    Map.of("asrProvider", session.getAsrProvider(), "ttsProvider", session.getTtsProvider())));
            return Mono.empty();
        }
        if (sessionService.getActiveSessionCount(context.userId) >= properties.getWebsocket().getMaxSessionsPerUser()) {
            return protocolFailure(context, "VOICE_SESSION_LIMIT", "Too many active voice sessions");
        }
        VoiceSession session = sessionService.createSession(message.threadId(), context.userId, null, null);
        context.voiceSession = session;
        context.sendControl(new VoiceProtocol.ControlMessage(
                "session.ready", session.getVoiceSessionId(), null, session.getThreadId(),
                null, null, null, null, null,
                Map.of("asrProvider", session.getAsrProvider(), "ttsProvider", session.getTtsProvider())));
        return Mono.empty();
    }

    private Mono<Void> startTurn(SessionContext context, VoiceProtocol.ControlMessage message) {
        if (context.voiceSession == null) {
            return protocolFailure(context, "VOICE_PROTOCOL_ERROR", "Voice session is not initialized");
        }
        if (context.activeTurn != null && !context.activeTurn.isTerminal()) {
            return protocolFailure(context, "VOICE_TURN_CONFLICT", "Another voice turn is still active");
        }
        if (message.turnId() == null || !SAFE_ID.matcher(message.turnId()).matches()) {
            return protocolFailure(context, "VOICE_PROTOCOL_ERROR", "turnId must contain 1-64 safe characters");
        }
        context.disposeTurnStreams();
        VoiceTurnCoordinator.ActiveTurn turn = turnCoordinator.createTurn(
                context.voiceSession, message.turnId(), null, null);
        context.activeTurn = turn;
        context.controlSubscription = turn.controlStream().subscribe(context::sendControl);
        context.audioSubscription = turn.audioStream().subscribe(context::sendAudio);
        return turn.start().onErrorResume(ex -> {
            log.error("Failed to start voice turn {} for session {}", message.turnId(), context.voiceSession == null ? null : context.voiceSession.getVoiceSessionId(), ex);
            return protocolFailure(context, "ASR_START_FAILED", "语音识别服务启动失败: " + safeMessage(ex));
        });
    }

    private Mono<Void> withMatchingTurn(SessionContext context, VoiceProtocol.ControlMessage message,
                                        java.util.function.Function<VoiceTurnCoordinator.ActiveTurn, Mono<Void>> action) {
        VoiceTurnCoordinator.ActiveTurn turn = context.activeTurn;
        if (turn == null || turn.isTerminal()) {
            return protocolFailure(context, "VOICE_TURN_NOT_ACTIVE", "No active voice turn");
        }
        if (!Objects.equals(message.turnId(), turn.getTurn().getTurnId())) {
            return protocolFailure(context, "VOICE_TURN_MISMATCH", "Control message does not match the active turn");
        }
        return action.apply(turn);
    }

    private Mono<Void> closeSession(SessionContext context) {
        Mono<Void> cancel = context.activeTurn == null || context.activeTurn.isTerminal()
                ? Mono.empty()
                : context.activeTurn.cancel("SESSION_CLOSED");
        return cancel.then(Mono.fromRunnable(() -> {
            context.disposeTurnStreams();
            if (context.voiceSession != null) {
                sessionService.closeSession(context.voiceSession.getVoiceSessionId());
                context.voiceSession = null;
            }
        }));
    }

    private Mono<Void> handleBinaryAudioFrame(SessionContext context, byte[] frameBytes) {
        if (context.activeTurn == null || context.activeTurn.isTerminal()) {
            return Mono.empty();
        }
        try {
            AudioChunk chunk = VoiceProtocol.decodeBinaryAudioFrame(frameBytes, AudioFormat.DEFAULT_INPUT);
            return context.activeTurn.appendAudio(chunk)
                    .onErrorResume(error -> protocolFailure(context, "VOICE_AUDIO_REJECTED", safeMessage(error)));
        } catch (IllegalArgumentException error) {
            return protocolFailure(context, "VOICE_FORMAT_UNSUPPORTED", safeMessage(error));
        }
    }

    private Mono<Void> protocolFailure(SessionContext context, String code, String message) {
        context.sendControl(new VoiceProtocol.ControlMessage(
                "error",
                context.voiceSession == null ? null : context.voiceSession.getVoiceSessionId(),
                context.activeTurn == null ? null : context.activeTurn.getTurn().getTurnId(),
                context.voiceSession == null ? null : context.voiceSession.getThreadId(),
                null, null, null, code, message, Map.of()));
        return Mono.empty();
    }

    private static String resolveUserId(WebSocketSession session) {
        String header = session.getHandshakeInfo().getHeaders().getFirst("X-User-Id");
        if (header != null && !header.isBlank()) return sanitizeUserId(header);
        String query = UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build().getQueryParams().getFirst("userId");
        return sanitizeUserId(query);
    }

    private static String sanitizeUserId(String value) {
        if (value == null || value.isBlank()) return "default-user";
        String sanitized = value.trim().replaceAll("[^A-Za-z0-9._@-]", "_");
        return sanitized.substring(0, Math.min(128, sanitized.length()));
    }

    private static String safeMessage(Throwable error) {
        if (error == null) return "语音服务操作失败";
        Throwable current = error;
        String message = null;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
                break;
            }
            current = current.getCause();
        }
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        if (message.contains("status code 400") || message.contains("CheckFailedException")) {
            message = "云端语音接口握手失败(HTTP 400)：请检查 DASHSCOPE_API_KEY 是否有效或模型名称/Endpoint配置";
        } else if (message.contains("status code 401") || message.contains("status code 403")) {
            message = "云端语音接口鉴权失败(HTTP 401/403)：请检查 API Key 权限";
        }
        message = message.replaceAll(
                "(?i)(bearer|api[-_ ]?key|access[-_ ]?token)\\s*[:=;]?\\s*\\S+", "$1 [redacted]");
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    private static final class SessionContext {
        private final String userId;
        private VoiceSession voiceSession;
        private VoiceTurnCoordinator.ActiveTurn activeTurn;
        private Disposable controlSubscription;
        private Disposable audioSubscription;
        private final Sinks.Many<Object> outboundSink = Sinks.many().unicast().onBackpressureBuffer();

        private SessionContext(String userId) { this.userId = userId; }

        private void sendControl(VoiceProtocol.ControlMessage message) {
            outboundSink.tryEmitNext(VoiceProtocol.encodeControlJson(message));
        }

        private void sendAudio(byte[] frame) { outboundSink.tryEmitNext(frame); }

        private void disposeTurnStreams() {
            if (controlSubscription != null) controlSubscription.dispose();
            if (audioSubscription != null) audioSubscription.dispose();
            controlSubscription = null;
            audioSubscription = null;
        }
    }
}
