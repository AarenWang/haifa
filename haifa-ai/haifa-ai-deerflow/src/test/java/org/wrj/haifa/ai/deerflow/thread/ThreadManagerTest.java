package org.wrj.haifa.ai.deerflow.thread;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadManagerTest {

    @Test
    void upsertsThreadAndKeepsMessagesOrdered() {
        ThreadManager threadManager = new ThreadManager();
        MessageStore messageStore = new MessageStore();

        ThreadRecord created = threadManager.upsert("thread-1", "Research Java agents", Map.of("source", "test"));
        threadManager.upsert("thread-1", "Ignored title", Map.of("source", "rerun"));

        messageStore.add(created.threadId(), "run-1", MessageRole.USER, "hello", Map.of());
        messageStore.add(created.threadId(), "run-1", MessageRole.ASSISTANT, "answer", Map.of());

        assertThat(threadManager.find("thread-1")).hasValueSatisfying(thread -> {
            assertThat(thread.title()).isEqualTo("Research Java agents");
            assertThat(thread.status()).isEqualTo(ThreadStatus.ACTIVE);
        });
        assertThat(threadManager.count()).isEqualTo(1);
        assertThat(messageStore.listByThread("thread-1")).extracting(MessageRecord::role)
                .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
    }
}
