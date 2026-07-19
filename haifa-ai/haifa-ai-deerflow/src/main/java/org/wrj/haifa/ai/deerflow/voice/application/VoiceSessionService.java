package org.wrj.haifa.ai.deerflow.voice.application;

import org.springframework.stereotype.Service;
import org.wrj.haifa.ai.deerflow.thread.ThreadManager;
import org.wrj.haifa.ai.deerflow.thread.ThreadRecord;
import org.wrj.haifa.ai.deerflow.voice.domain.VoiceSession;
import org.wrj.haifa.ai.deerflow.voice.domain.VoiceSessionStatus;
import org.wrj.haifa.ai.deerflow.voice.VoiceProperties;
import org.wrj.haifa.ai.deerflow.voice.observability.VoiceObservabilityService;
import org.wrj.haifa.ai.deerflow.voice.persistence.entity.VoiceSessionEntity;
import org.wrj.haifa.ai.deerflow.voice.persistence.repository.VoiceSessionRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VoiceSessionService {

    private final ThreadManager threadManager;
    private final VoiceSessionRepository sessionRepository;
    private final Map<String, VoiceSession> activeSessions = new ConcurrentHashMap<>();

    private final VoiceProperties properties;
    private final VoiceObservabilityService observability;

    public VoiceSessionService(ThreadManager threadManager,
                               VoiceSessionRepository sessionRepository,
                               VoiceProperties properties,
                               VoiceObservabilityService observability) {
        this.threadManager = threadManager;
        this.sessionRepository = sessionRepository;
        this.properties = properties;
        this.observability = observability;
    }

    public VoiceSession createSession(String requestedThreadId, String userId, String asrPref, String ttsPref) {
        String threadId = requestedThreadId;
        if (threadId == null || threadId.isBlank()) {
            ThreadRecord created = threadManager.create("Voice Conversation", Map.of("source", "voice"));
            threadId = created.threadId();
        }

        String sessionId = "vs_" + UUID.randomUUID().toString().replace("-", "");
        String asr = (asrPref != null && !asrPref.isBlank())
                ? asrPref : properties.getAsr().getDefaultProvider();
        String tts = (ttsPref != null && !ttsPref.isBlank())
                ? ttsPref : properties.getTts().getDefaultProvider();

        VoiceSession session = new VoiceSession(sessionId, threadId, userId, asr, tts);
        activeSessions.put(sessionId, session);
        observability.incrementActiveSessions();

        // Persist session entity to DB
        VoiceSessionEntity entity = new VoiceSessionEntity();
        entity.setVoiceSessionId(sessionId);
        entity.setThreadId(threadId);
        entity.setUserId(userId != null ? userId : "anonymous");
        entity.setStatus(VoiceSessionStatus.ACTIVE.name());
        entity.setLocale(session.getLocale());
        entity.setAsrProvider(asr);
        entity.setTtsProvider(tts);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        sessionRepository.save(entity);

        return session;
    }

    public Optional<VoiceSession> getSession(String sessionId) {
        return Optional.ofNullable(activeSessions.get(sessionId));
    }

    public void closeSession(String sessionId) {
        VoiceSession session = activeSessions.remove(sessionId);
        if (session != null) {
            observability.decrementActiveSessions();
            session.setStatus(VoiceSessionStatus.CLOSED);
            sessionRepository.findById(sessionId).ifPresent(entity -> {
                entity.setStatus(VoiceSessionStatus.CLOSED.name());
                entity.setClosedAt(Instant.now());
                entity.setUpdatedAt(Instant.now());
                sessionRepository.save(entity);
            });
        }
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public long getActiveSessionCount(String userId) {
        return activeSessions.values().stream()
                .filter(session -> session.getUserId().equals(userId))
                .count();
    }
}
