package org.wrj.haifa.ai.deerflow.persistence.store;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class AgentClarificationStoreTest {

    private final AgentClarificationStore store = new AgentClarificationStore();

    @Test
    void testCreatePendingAndAnswerAndCancel() {
        // Create pending clarification
        ClarificationRecord record1 = store.create("thread-1", "run-1", "Is X correct?", "missing_info", "context-1");
        assertThat(record1.clarificationId()).isNotBlank();
        assertThat(record1.threadId()).isEqualTo("thread-1");
        assertThat(record1.runId()).isEqualTo("run-1");
        assertThat(record1.question()).isEqualTo("Is X correct?");
        assertThat(record1.clarificationType()).isEqualTo("missing_info");
        assertThat(record1.context()).isEqualTo("context-1");
        assertThat(record1.status()).isEqualTo(ClarificationStatus.PENDING);
        assertThat(record1.answer()).isNull();
        assertThat(record1.createdAt()).isNotNull();
        assertThat(record1.answeredAt()).isNull();

        // Find pending
        Optional<ClarificationRecord> pending = store.findPending("thread-1");
        assertThat(pending).isPresent().contains(record1);

        Optional<ClarificationRecord> pendingByRun = store.findPendingByRunId("run-1");
        assertThat(pendingByRun).isPresent().contains(record1);

        // Answer
        ClarificationRecord answered = store.answer(record1.clarificationId(), "Yes");
        assertThat(answered.status()).isEqualTo(ClarificationStatus.ANSWERED);
        assertThat(answered.answer()).isEqualTo("Yes");
        assertThat(answered.answeredAt()).isNotNull();

        assertThat(store.findPending("thread-1")).isEmpty();

        // Create another, then cancel
        ClarificationRecord record2 = store.create("thread-1", "run-2", "Is Y correct?", "missing_info", "");
        assertThat(store.findPending("thread-1")).isPresent();

        store.cancel(record2.clarificationId());
        assertThat(store.findPending("thread-1")).isEmpty();
        assertThat(store.find(record2.clarificationId())).hasValueSatisfying(c -> {
            assertThat(c.status()).isEqualTo(ClarificationStatus.CANCELLED);
        });
    }
}
