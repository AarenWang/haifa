package org.wrj.haifa.ai.deerflow.run;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RunManagerTest {

    @Test
    void tracksRunStatusTransitions() {
        RunManager manager = new RunManager();

        RunRecord created = manager.create("thread-1", "test-model", Map.of("source", "test"));
        assertThat(created.status()).isEqualTo(RunStatus.PENDING);

        RunRecord running = manager.markRunning(created.runId());
        assertThat(running.status()).isEqualTo(RunStatus.RUNNING);

        RunRecord completed = manager.markCompleted(created.runId());
        assertThat(completed.status()).isEqualTo(RunStatus.COMPLETED);

        assertThat(manager.find(created.runId())).hasValueSatisfying(record -> {
            assertThat(record.threadId()).isEqualTo("thread-1");
            assertThat(record.modelName()).isEqualTo("test-model");
            assertThat(record.status()).isEqualTo(RunStatus.COMPLETED);
        });
    }
}
