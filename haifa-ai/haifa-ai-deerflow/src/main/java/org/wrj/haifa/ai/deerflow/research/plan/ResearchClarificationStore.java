package org.wrj.haifa.ai.deerflow.research.plan;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;

/**
 * Stores pending clarification prompts per thread so a follow-up user reply can
 * resume the research flow with the original context.
 */
@Component
public class ResearchClarificationStore {

    private final Map<String, PendingClarification> pendingByThreadId = new ConcurrentHashMap<>();

    public PendingClarification save(String threadId, String runId, String originalMessage,
            ResearchOptions researchOptions, String question, String clarificationType) {
        PendingClarification clarification = new PendingClarification(
                threadId,
                runId,
                originalMessage,
                researchOptions,
                question,
                clarificationType,
                Instant.now()
        );
        pendingByThreadId.put(threadId, clarification);
        return clarification;
    }

    public Optional<PendingClarification> find(String threadId) {
        return Optional.ofNullable(pendingByThreadId.get(threadId));
    }

    public Optional<PendingClarification> consume(String threadId) {
        return Optional.ofNullable(pendingByThreadId.remove(threadId));
    }

    public void clear(String threadId) {
        pendingByThreadId.remove(threadId);
    }

    public record PendingClarification(
            String threadId,
            String runId,
            String originalMessage,
            ResearchOptions researchOptions,
            String question,
            String clarificationType,
            Instant createdAt
    ) {
    }
}
