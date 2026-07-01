package org.wrj.haifa.ai.deerflow.persistence.store;

import java.util.Optional;

public interface ClarificationStore {
    ClarificationRecord create(String threadId, String runId, String question, String type, String context);
    Optional<ClarificationRecord> findPending(String threadId);
    Optional<ClarificationRecord> findPendingByRunId(String runId);
    Optional<ClarificationRecord> findByRunId(String runId);
    Optional<ClarificationRecord> find(String clarificationId);
    ClarificationRecord answer(String clarificationId, String answer);
    void cancel(String clarificationId);
}
