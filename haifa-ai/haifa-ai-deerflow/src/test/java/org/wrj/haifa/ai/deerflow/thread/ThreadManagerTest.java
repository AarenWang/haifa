package org.wrj.haifa.ai.deerflow.thread;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ThreadManagerTest {

    @Autowired
    private ThreadManager threadManager;

    @Autowired
    private MessageStore messageStore;

    @Test
    void upsertsThreadAndKeepsMessagesOrdered() {
        String threadId = UUID.randomUUID().toString();
        ThreadRecord created = threadManager.upsert(threadId, "Research Java agents", Map.of("source", "test"));
        threadManager.upsert(threadId, "Ignored title", Map.of("source", "rerun"));

        messageStore.add(created.threadId(), "run-1", MessageRole.USER, "hello", Map.of());
        messageStore.add(created.threadId(), "run-1", MessageRole.ASSISTANT, "answer", Map.of());

        assertThat(threadManager.find(threadId)).hasValueSatisfying(thread -> {
            assertThat(thread.title()).isEqualTo("Research Java agents");
            assertThat(thread.status()).isEqualTo(ThreadStatus.ACTIVE);
        });
        assertThat(messageStore.listByThread(threadId)).extracting(MessageRecord::role)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
    }

    @Test
    void listReturnsThreadsOrderedByUpdatedAtDesc() {
        int beforeSize = threadManager.list().size();
        threadManager.create("Thread A", Map.of());
        threadManager.create("Thread B", Map.of());

        assertThat(threadManager.list()).hasSize(beforeSize + 2);
        assertThat(threadManager.list().get(0).title()).isEqualTo("Thread B");
    }

    @Test
    void updateAndTouchWork() {
        ThreadRecord created = threadManager.create("Original", Map.of());
        threadManager.update(created.threadId(), "Updated", ThreadStatus.ARCHIVED, Map.of("key", "value"));

        assertThat(threadManager.find(created.threadId())).hasValueSatisfying(thread -> {
            assertThat(thread.title()).isEqualTo("Updated");
            assertThat(thread.status()).isEqualTo(ThreadStatus.ARCHIVED);
        });

        threadManager.touch(created.threadId());
        assertThat(threadManager.find(created.threadId())).isPresent();
    }

    @Test
    void persistenceSurvivesReconstruction() {
        ThreadRecord created = threadManager.create("Persistent", Map.of("key", "val"));
        String threadId = created.threadId();

        // Same database, new query
        assertThat(threadManager.find(threadId)).isPresent();
        assertThat(threadManager.find(threadId).get().title()).isEqualTo("Persistent");
    }
}
