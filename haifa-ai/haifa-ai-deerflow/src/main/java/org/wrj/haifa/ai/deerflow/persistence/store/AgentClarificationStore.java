package org.wrj.haifa.ai.deerflow.persistence.store;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AgentClarificationStore implements ClarificationStore {

    private final Map<String, ClarificationRecord> store = new ConcurrentHashMap<>();

    @Override
    public ClarificationRecord create(String threadId, String runId, String question, String type, String context) {
        findPending(threadId).ifPresent(c -> cancel(c.clarificationId()));

        String clarificationId = UUID.randomUUID().toString();
        ClarificationRecord record = new ClarificationRecord(
                clarificationId,
                threadId,
                runId,
                question,
                type,
                context,
                ClarificationStatus.PENDING,
                null,
                Instant.now(),
                null
        );
        store.put(clarificationId, record);
        return record;
    }

    @Override
    public Optional<ClarificationRecord> findPending(String threadId) {
        if (threadId == null) {
            return Optional.empty();
        }
        return store.values().stream()
                .filter(c -> threadId.equals(c.threadId()) && c.status() == ClarificationStatus.PENDING)
                .findFirst();
    }

    @Override
    public Optional<ClarificationRecord> findPendingByRunId(String runId) {
        if (runId == null) {
            return Optional.empty();
        }
        return store.values().stream()
                .filter(c -> runId.equals(c.runId()) && c.status() == ClarificationStatus.PENDING)
                .findFirst();
    }

    @Override
    public Optional<ClarificationRecord> findByRunId(String runId) {
        if (runId == null) {
            return Optional.empty();
        }
        return store.values().stream()
                .filter(c -> runId.equals(c.runId()))
                .findFirst();
    }

    @Override
    public Optional<ClarificationRecord> find(String clarificationId) {
        if (clarificationId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(clarificationId));
    }

    @Override
    public ClarificationRecord answer(String clarificationId, String answer) {
        ClarificationRecord old = store.get(clarificationId);
        if (old == null) {
            throw new IllegalArgumentException("Clarification not found: " + clarificationId);
        }
        if (old.status() != ClarificationStatus.PENDING) {
            throw new IllegalStateException("Clarification " + clarificationId + " is not in PENDING status: " + old.status());
        }
        ClarificationRecord updated = new ClarificationRecord(
                old.clarificationId(),
                old.threadId(),
                old.runId(),
                old.question(),
                old.clarificationType(),
                old.context(),
                ClarificationStatus.ANSWERED,
                answer,
                old.createdAt(),
                Instant.now()
        );
        store.put(clarificationId, updated);
        return updated;
    }

    @Override
    public void cancel(String clarificationId) {
        ClarificationRecord old = store.get(clarificationId);
        if (old != null) {
            ClarificationRecord updated = new ClarificationRecord(
                    old.clarificationId(),
                    old.threadId(),
                    old.runId(),
                    old.question(),
                    old.clarificationType(),
                    old.context(),
                    ClarificationStatus.CANCELLED,
                    old.answer(),
                    old.createdAt(),
                    Instant.now()
            );
            store.put(clarificationId, updated);
        }
    }

    public void clearAll() {
        store.clear();
    }
}
