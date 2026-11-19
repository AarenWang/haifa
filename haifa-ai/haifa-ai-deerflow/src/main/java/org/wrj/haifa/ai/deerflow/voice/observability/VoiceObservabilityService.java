package org.wrj.haifa.ai.deerflow.voice.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class VoiceObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(VoiceObservabilityService.class);

    private final AtomicLong activeSessionCount = new AtomicLong(0);
    private final AtomicLong totalTurnsCount = new AtomicLong(0);
    private final AtomicLong totalErrorsCount = new AtomicLong(0);

    public void incrementActiveSessions() {
        activeSessionCount.incrementAndGet();
    }

    public void decrementActiveSessions() {
        activeSessionCount.updateAndGet(value -> Math.max(0, value - 1));
    }

    public void recordTurnCompleted(String turnId, long durationMs) {
        totalTurnsCount.incrementAndGet();
        log.info("Voice turn completed: turnId={}, durationMs={}", turnId, durationMs);
    }

    public void recordError(String turnId, String errorCode) {
        totalErrorsCount.incrementAndGet();
        log.warn("Voice turn error: turnId={}, errorCode={}", turnId, errorCode);
    }

    public long getActiveSessions() {
        return activeSessionCount.get();
    }

    public long getTotalTurns() {
        return totalTurnsCount.get();
    }

    public long getTotalErrors() {
        return totalErrorsCount.get();
    }
}
