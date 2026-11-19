package org.wrj.haifa.ai.deerflow.persistence.store;

import java.util.Optional;

public interface ClarificationStore {
    ClarificationRecord create(String threadId, String runId, String question, String type, String context,
                               java.util.List<String> options, java.util.List<ClarificationQuestion> questions);

    default ClarificationRecord create(String threadId, String runId, String question, String type, String context,
                                       java.util.List<String> options) {
        return create(threadId, runId, question, type, context, options, java.util.List.of());
    }

    default ClarificationRecord create(String threadId, String runId, String question, String type, String context) {
        return create(threadId, runId, question, type, context, java.util.List.of());
    }
    Optional<ClarificationRecord> findPending(String threadId);
    Optional<ClarificationRecord> findPendingByRunId(String runId);
    Optional<ClarificationRecord> findByRunId(String runId);
    Optional<ClarificationRecord> find(String clarificationId);
    ClarificationRecord answer(String clarificationId, String answer, java.util.List<ClarificationAnswer> answers);

    default ClarificationRecord answer(String clarificationId, String answer) {
        return answer(clarificationId, answer, java.util.List.of());
    }

    void cancel(String clarificationId);
}
