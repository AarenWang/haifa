package org.wrj.haifa.ai.deerflow.voice.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRuntime;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.run.RunCancellationService;
import org.wrj.haifa.ai.deerflow.voice.VoiceProperties;
import org.wrj.haifa.ai.deerflow.voice.domain.AsrEvent;
import org.wrj.haifa.ai.deerflow.voice.domain.AsrEventType;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioChunk;
import org.wrj.haifa.ai.deerflow.voice.domain.AudioFormat;
import org.wrj.haifa.ai.deerflow.voice.domain.TextChunk;
import org.wrj.haifa.ai.deerflow.voice.domain.VoiceSession;
import org.wrj.haifa.ai.deerflow.voice.domain.VoiceTurn;
import org.wrj.haifa.ai.deerflow.voice.domain.VoiceTurnStatus;
import org.wrj.haifa.ai.deerflow.voice.persistence.entity.VoiceTurnEntity;
import org.wrj.haifa.ai.deerflow.voice.persistence.repository.VoiceTurnRepository;
import org.wrj.haifa.ai.deerflow.voice.provider.AsrSession;
import org.wrj.haifa.ai.deerflow.voice.provider.AsrStartOptions;
import org.wrj.haifa.ai.deerflow.voice.provider.StreamingAsrProvider;
import org.wrj.haifa.ai.deerflow.voice.provider.StreamingTtsProvider;
import org.wrj.haifa.ai.deerflow.voice.provider.TtsSession;
import org.wrj.haifa.ai.deerflow.voice.provider.TtsStartOptions;
import org.wrj.haifa.ai.deerflow.voice.provider.VoiceProviderResolver;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class VoiceTurnCoordinator {

    private static final Logger log = LoggerFactory.getLogger(VoiceTurnCoordinator.class);

    private final AgentRuntime agentRuntime;
    private final RunCancellationService cancellationService;
    private final VoiceProviderResolver providerResolver;
    private final VoiceTurnRepository turnRepository;
    private final VoiceProperties properties;

    public VoiceTurnCoordinator(AgentRuntime agentRuntime,
                                RunCancellationService cancellationService,
                                VoiceProviderResolver providerResolver,
                                VoiceTurnRepository turnRepository,
                                VoiceProperties properties) {
        this.agentRuntime = agentRuntime;
        this.cancellationService = cancellationService;
        this.providerResolver = providerResolver;
        this.turnRepository = turnRepository;
        this.properties = properties;
    }

    public ActiveTurn createTurn(VoiceSession session, String turnId, String asrOverride, String ttsOverride) {
        StreamingAsrProvider asrProvider = providerResolver.resolveAsr(asrOverride, session.getAsrProvider());
        StreamingTtsProvider ttsProvider = providerResolver.resolveTts(ttsOverride, session.getTtsProvider());
        VoiceTurn turn = new VoiceTurn(turnId, session.getVoiceSessionId(), asrProvider.id(), ttsProvider.id());
        turn.setAsrModel(asrProvider.model());
        turn.setTtsModel(ttsProvider.model());
        return new ActiveTurn(session, turn, asrProvider, ttsProvider, agentRuntime,
                cancellationService, turnRepository, properties);
    }

    public static class ActiveTurn {
        private final VoiceSession session;
        private final VoiceTurn turn;
        private final StreamingAsrProvider asrProvider;
        private final StreamingTtsProvider ttsProvider;
        private final AgentRuntime agentRuntime;
        private final RunCancellationService cancellationService;
        private final VoiceTurnRepository turnRepository;
        private final VoiceProperties properties;
        private final Instant startedAt = Instant.now();

        private volatile AsrSession asrSession;
        private volatile TtsSession ttsSession;
        private volatile Disposable asrSubscription;
        private volatile Disposable agentSubscription;
        private volatile Disposable ttsTextSubscription;
        private volatile Disposable ttsAudioSubscription;

        private final TtsTextChunker chunker = new TtsTextChunker();
        private final StringBuilder rawAgentText = new StringBuilder();
        private String normalizedAgentText = "";
        private final Sinks.Many<TextChunk> ttsTextSink = Sinks.many().unicast().onBackpressureBuffer();
        private final Sinks.Many<VoiceProtocol.ControlMessage> controlSink = Sinks.many().replay().limit(64);
        private final Sinks.Many<byte[]> audioSink = Sinks.many().unicast().onBackpressureBuffer();

        private final AtomicBoolean active = new AtomicBoolean(true);
        private final AtomicBoolean committed = new AtomicBoolean();
        private final AtomicBoolean agentStarted = new AtomicBoolean();
        private final AtomicBoolean agentDone = new AtomicBoolean();
        private final AtomicBoolean ttsDone = new AtomicBoolean();
        private final AtomicBoolean terminalEventSent = new AtomicBoolean();
        private final AtomicLong receivedBytes = new AtomicLong();
        private final AtomicLong lastInputSequence = new AtomicLong();

        ActiveTurn(VoiceSession session, VoiceTurn turn,
                   StreamingAsrProvider asrProvider, StreamingTtsProvider ttsProvider,
                   AgentRuntime agentRuntime, RunCancellationService cancellationService,
                   VoiceTurnRepository turnRepository, VoiceProperties properties) {
            this.session = session;
            this.turn = turn;
            this.asrProvider = asrProvider;
            this.ttsProvider = ttsProvider;
            this.agentRuntime = agentRuntime;
            this.cancellationService = cancellationService;
            this.turnRepository = turnRepository;
            this.properties = properties;
            saveTurnToDb();
        }

        public VoiceTurn getTurn() { return turn; }
        public boolean isTerminal() { return !active.get(); }
        public Flux<VoiceProtocol.ControlMessage> controlStream() { return controlSink.asFlux(); }
        public Flux<byte[]> audioStream() { return audioSink.asFlux(); }

        public Mono<Void> start() {
            if (!active.get()) return Mono.empty();
            turn.setStatus(VoiceTurnStatus.LISTENING);
            saveTurnToDb();
            AsrStartOptions options = new AsrStartOptions(
                    session.getLocale(), AudioFormat.DEFAULT_INPUT, null, Map.of());
            return asrProvider.open(options)
                    .doOnNext(openedSession -> {
                        if (!active.get()) {
                            openedSession.cancel().subscribe();
                            return;
                        }
                        asrSession = openedSession;
                        asrSubscription = openedSession.events().subscribe(
                                this::handleAsrEvent, this::handleAsrError);
                        emitControl(new VoiceProtocol.ControlMessage(
                                "asr.ready", session.getVoiceSessionId(), turn.getTurnId(), session.getThreadId(),
                                null, null, null, null, null,
                                Map.of("provider", asrProvider.id(), "model", asrProvider.model())));
                    })
                    .onErrorResume(error -> fail("ASR_UNAVAILABLE", error).then(Mono.empty()))
                    .then();
        }

        public Mono<Void> appendAudio(AudioChunk chunk) {
            if (!active.get()) return Mono.empty();
            if (asrSession == null) return Mono.error(new IllegalStateException("ASR session is not ready"));
            if (committed.get()) return Mono.error(new IllegalStateException("Voice turn is already committed"));
            if (chunk.sequence() <= lastInputSequence.get()) {
                return Mono.error(new IllegalArgumentException("Audio sequence must increase monotonically"));
            }
            byte[] pcmData = chunk.data();
            long totalBytes = receivedBytes.addAndGet(pcmData.length);
            if (totalBytes > properties.getAudio().getMaxTurnBytes()) {
                return fail("VOICE_TURN_TOO_LARGE", new IllegalArgumentException("Voice turn exceeded maximum audio bytes"));
            }
            if (Duration.between(startedAt, Instant.now()).compareTo(properties.getAudio().getMaxTurnDuration()) > 0) {
                return fail("VOICE_TURN_TOO_LONG", new IllegalArgumentException("Voice turn exceeded maximum duration"));
            }
            lastInputSequence.set(chunk.sequence());
            return asrSession.append(pcmData);
        }

        public Mono<Void> commit() {
            if (!active.get() || !committed.compareAndSet(false, true)) return Mono.empty();
            if (asrSession == null) return fail("ASR_UNAVAILABLE", new IllegalStateException("ASR session is not ready"));
            if (receivedBytes.get() == 0) return fail("ASR_EMPTY_AUDIO", new IllegalArgumentException("No audio was received"));
            turn.setStatus(VoiceTurnStatus.TRANSCRIBING);
            turn.setInputDurationMs(Duration.between(startedAt, Instant.now()).toMillis());
            saveTurnToDb();
            return asrSession.commit().onErrorResume(error -> fail("ASR_ERROR", error));
        }

        public Mono<Void> cancel(String reason) {
            if (!active.compareAndSet(true, false)) return Mono.empty();
            disposeSubscriptions();
            chunker.reset();
            ttsTextSink.tryEmitComplete();
            turn.setStatus(VoiceTurnStatus.CANCELLED);
            saveTurnToDb();
            if (turn.getRunId() != null) cancellationService.requestCancel(turn.getRunId(), reason);

            Mono<Void> cancelAsr = asrSession == null ? Mono.empty() : asrSession.cancel().onErrorResume(e -> Mono.empty());
            Mono<Void> cancelTts = ttsSession == null ? Mono.empty() : ttsSession.cancel().onErrorResume(e -> Mono.empty());
            return Mono.whenDelayError(cancelAsr, cancelTts).doFinally(signal -> {
                emitControl(new VoiceProtocol.ControlMessage(
                        "turn.cancelled", session.getVoiceSessionId(), turn.getTurnId(), session.getThreadId(),
                        null, turn.getRunId(), reason, null, null, Map.of()));
                completeOutbound();
            });
        }

        private void handleAsrEvent(AsrEvent event) {
            if (!active.get()) return;
            if (event.type() == AsrEventType.PARTIAL) {
                if (turn.getFirstAsrMs() == null) turn.setFirstAsrMs(elapsedMs());
                emitControl(new VoiceProtocol.ControlMessage(
                        "asr.partial", session.getVoiceSessionId(), turn.getTurnId(), session.getThreadId(),
                        event.text(), null, null, null, null, Map.of()));
                return;
            }
            if (event.type() != AsrEventType.FINAL || !agentStarted.compareAndSet(false, true)) return;

            String finalText = event.text() == null ? "" : event.text().trim();
            if (finalText.isBlank()) {
                fail("ASR_EMPTY_RESULT", new IllegalArgumentException("Empty transcript received")).subscribe();
                return;
            }
            if (finalText.length() > 1000) {
                fail("ASR_RESULT_TOO_LONG", new IllegalArgumentException("Transcript exceeds 1000 characters")).subscribe();
                return;
            }
            turn.setFinalAsrMs(elapsedMs());
            turn.setTranscriptCharCount(finalText.length());
            saveTurnToDb();
            emitControl(new VoiceProtocol.ControlMessage(
                    "asr.final", session.getVoiceSessionId(), turn.getTurnId(), session.getThreadId(),
                    finalText, null, null, null, null, Map.of()));
            startAgentRun(finalText);
        }

        private void handleAsrError(Throwable error) {
            if (active.get()) fail("ASR_ERROR", error).subscribe();
        }

        private void startAgentRun(String finalText) {
            turn.setStatus(VoiceTurnStatus.WAITING_AGENT);
            saveTurnToDb();

            Mono<TtsSession> ttsReady = ttsProvider.open(new TtsStartOptions(
                            null, 1.0, null, AudioFormat.DEFAULT_OUTPUT, Map.of()))
                    .cache();

            ttsTextSubscription = ttsReady
                    .doOnNext(opened -> {
                        ttsSession = opened;
                        AudioFormat outputFormat = opened.format();
                        emitControl(new VoiceProtocol.ControlMessage(
                                "tts.started", session.getVoiceSessionId(), turn.getTurnId(), session.getThreadId(),
                                null, turn.getRunId(), null, null, null,
                                Map.of("provider", ttsProvider.id(), "model", ttsProvider.model(),
                                        "codec", outputFormat.codec(),
                                        "sampleRateHz", outputFormat.sampleRateHz())));
                        ttsAudioSubscription = opened.audio().subscribe(
                                this::handleAudioChunk,
                                error -> handleTtsFailure("TTS_STREAM_FAILED", error),
                                () -> { ttsDone.set(true); tryCompleteTurn(); });
                    })
                    .flatMapMany(opened -> ttsTextSink.asFlux()
                            .concatMap(opened::append, 1)
                            .then(opened.commit())
                            .thenMany(Flux.empty()))
                    .subscribe(null, error -> handleTtsFailure("TTS_UNAVAILABLE", error));

            AgentRequest request = new AgentRequest(session.getThreadId(), finalText, null,
                    List.of(), RunMode.CHAT, ResearchOptions.defaults(), session.getUserId());
            agentSubscription = agentRuntime.stream(request).subscribe(
                    this::handleAgentEvent,
                    this::handleAgentError,
                    this::handleAgentComplete);
        }

        private void handleAgentEvent(AgentEvent event) {
            if (!active.get()) return;
            if (turn.getRunId() == null && event.runId() != null) {
                turn.setRunId(event.runId());
                saveTurnToDb();
                emitControl(new VoiceProtocol.ControlMessage(
                        "agent.started", session.getVoiceSessionId(), turn.getTurnId(), session.getThreadId(),
                        null, event.runId(), null, null, null, Map.of()));
            }
            if (event.type() != AgentEventType.MODEL_DELTA || event.content() == null) return;

            if (turn.getFirstAgentTokenMs() == null) turn.setFirstAgentTokenMs(elapsedMs());
            rawAgentText.append(event.content());
            turn.setOutputCharCount(rawAgentText.length());
            emitControl(new VoiceProtocol.ControlMessage(
                    "agent.delta", session.getVoiceSessionId(), turn.getTurnId(), session.getThreadId(),
                    event.content(), turn.getRunId(), null, null, null, Map.of()));

            String normalized = SpeechTextNormalizer.normalize(rawAgentText.toString());
            if (normalized.startsWith(normalizedAgentText)) {
                String delta = normalized.substring(normalizedAgentText.length());
                normalizedAgentText = normalized;
                chunker.offer(delta).forEach(this::queueTtsChunk);
            }
        }

        private void handleAgentError(Throwable error) {
            if (active.get()) fail("AGENT_FAILED", error).subscribe();
        }

        private void handleAgentComplete() {
            if (!active.get()) return;
            String finalNormalized = SpeechTextNormalizer.normalize(rawAgentText.toString());
            if (finalNormalized.startsWith(normalizedAgentText)) {
                chunker.offer(finalNormalized.substring(normalizedAgentText.length())).forEach(this::queueTtsChunk);
            }
            chunker.flush().forEach(this::queueTtsChunk);
            ttsTextSink.tryEmitComplete();
            agentDone.set(true);
            saveTurnToDb();
            tryCompleteTurn();
        }

        private void queueTtsChunk(TextChunk chunk) {
            if (!active.get() || chunk.text().isBlank()) return;
            Sinks.EmitResult result = ttsTextSink.tryEmitNext(chunk);
            if (result.isFailure()) handleTtsFailure("TTS_BACKPRESSURE", new IllegalStateException(result.toString()));
        }

        private void handleAudioChunk(AudioChunk chunk) {
            if (!active.get()) return;
            if (turn.getFirstTtsAudioMs() == null) turn.setFirstTtsAudioMs(elapsedMs());
            if (turn.getStatus() != VoiceTurnStatus.SPEAKING) {
                turn.setStatus(VoiceTurnStatus.SPEAKING);
                saveTurnToDb();
            }
            audioSink.tryEmitNext(VoiceProtocol.encodeBinaryAudioFrame(chunk));
        }

        private void handleTtsFailure(String code, Throwable error) {
            if (!active.get() || ttsDone.getAndSet(true)) return;
            log.warn("{} during voice turn {}: {}", code, turn.getTurnId(), safeMessage(error));
            ttsTextSink.tryEmitComplete();
            if (ttsTextSubscription != null) ttsTextSubscription.dispose();
            TtsSession opened = ttsSession;
            if (opened != null) opened.cancel().onErrorResume(ignored -> Mono.empty()).subscribe();
            emitControl(new VoiceProtocol.ControlMessage(
                    "tts.error", session.getVoiceSessionId(), turn.getTurnId(), session.getThreadId(),
                    null, turn.getRunId(), null, code, "语音朗读失败，文本回答仍然可用。", Map.of()));
            tryCompleteTurn();
        }

        private void tryCompleteTurn() {
            if (!active.get() || !agentDone.get() || !ttsDone.get() || !terminalEventSent.compareAndSet(false, true)) return;
            active.set(false);
            turn.setStatus(VoiceTurnStatus.COMPLETED);
            saveTurnToDb();
            emitControl(new VoiceProtocol.ControlMessage(
                    "turn.completed", session.getVoiceSessionId(), turn.getTurnId(), session.getThreadId(),
                    null, turn.getRunId(), null, null, null, Map.of()));
            completeOutbound();
        }

        private Mono<Void> fail(String code, Throwable error) {
            if (!active.compareAndSet(true, false)) return Mono.empty();
            log.error("Voice turn {} failed with {}: {}", turn.getTurnId(), code, safeMessage(error), error);
            disposeSubscriptions();
            ttsTextSink.tryEmitComplete();
            turn.setErrorCode(code);
            turn.setStatus(VoiceTurnStatus.ERROR);
            saveTurnToDb();
            emitControl(new VoiceProtocol.ControlMessage(
                    "error", session.getVoiceSessionId(), turn.getTurnId(), session.getThreadId(),
                    null, turn.getRunId(), null, code, safeMessage(error), Map.of()));
            Mono<Void> cancelAsr = asrSession == null ? Mono.empty() : asrSession.cancel().onErrorResume(e -> Mono.empty());
            Mono<Void> cancelTts = ttsSession == null ? Mono.empty() : ttsSession.cancel().onErrorResume(e -> Mono.empty());
            return Mono.whenDelayError(cancelAsr, cancelTts).doFinally(signal -> completeOutbound());
        }

        private void disposeSubscriptions() {
            if (asrSubscription != null) asrSubscription.dispose();
            if (agentSubscription != null) agentSubscription.dispose();
            if (ttsTextSubscription != null) ttsTextSubscription.dispose();
            if (ttsAudioSubscription != null) ttsAudioSubscription.dispose();
        }

        private void completeOutbound() {
            controlSink.tryEmitComplete();
            audioSink.tryEmitComplete();
        }

        private void emitControl(VoiceProtocol.ControlMessage message) {
            controlSink.tryEmitNext(message);
        }

        private long elapsedMs() { return Duration.between(startedAt, Instant.now()).toMillis(); }

        private static String safeMessage(Throwable error) {
            log.error("Error occurred while processing voice turn", error);
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
            message = message.replaceAll("(?i)(bearer|api[-_ ]?key|access[-_ ]?token)\\s*[:=;]?\\s*\\S+", "$1 [redacted]");
            return message.length() > 300 ? message.substring(0, 300) : message;
        }

        private void saveTurnToDb() {
            try {
                VoiceTurnEntity entity = turnRepository.findById(turn.getTurnId()).orElseGet(VoiceTurnEntity::new);
                entity.setTurnId(turn.getTurnId());
                entity.setVoiceSessionId(turn.getVoiceSessionId());
                entity.setRunId(turn.getRunId());
                entity.setStatus(turn.getStatus().name());
                entity.setInputDurationMs(turn.getInputDurationMs());
                entity.setTranscriptCharCount(turn.getTranscriptCharCount());
                entity.setOutputCharCount(turn.getOutputCharCount());
                entity.setAsrProvider(turn.getAsrProvider());
                entity.setAsrModel(turn.getAsrModel());
                entity.setTtsProvider(turn.getTtsProvider());
                entity.setTtsModel(turn.getTtsModel());
                entity.setFirstAsrMs(turn.getFirstAsrMs());
                entity.setFinalAsrMs(turn.getFinalAsrMs());
                entity.setFirstAgentTokenMs(turn.getFirstAgentTokenMs());
                entity.setFirstTtsAudioMs(turn.getFirstTtsAudioMs());
                entity.setErrorCode(turn.getErrorCode());
                entity.setCreatedAt(turn.getCreatedAt());
                entity.setUpdatedAt(Instant.now());
                entity.setCompletedAt(turn.getCompletedAt());
                turnRepository.save(entity);
            } catch (Exception ex) {
                log.warn("Failed to persist voice turn {}: {}", turn.getTurnId(), ex.getMessage());
            }
        }
    }
}
