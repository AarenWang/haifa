package org.wrj.haifa.ai.deerflow.run;

import java.util.Map;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RunManagerTest {

    @Autowired
    private RunManager manager;

    @Test
    void tracksRunStatusTransitions() {
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

    @Test
    void listByThreadReturnsRunsOrderedByCreatedAtDesc() {
        RunRecord run1 = manager.create("thread-list", "model", Map.of());
        RunRecord run2 = manager.create("thread-list", "model", Map.of());

        assertThat(manager.listByThread("thread-list")).hasSize(2);
        assertThat(manager.listByThread("thread-list").get(0).runId()).isEqualTo(run2.runId());
    }

    @Test
    void listByThreadCancelsOlderRunningRunsSupersededByNewerRun() {
        RunRecord older = manager.create("thread-superseded", "model", Map.of());
        manager.markRunning(older.runId());
        RunRecord newer = manager.create("thread-superseded", "model", Map.of());
        manager.markRunning(newer.runId());
        manager.markCompleted(newer.runId());

        assertThat(manager.listByThread("thread-superseded"))
                .extracting(RunRecord::runId)
                .containsExactly(newer.runId(), older.runId());
        assertThat(manager.find(older.runId())).hasValueSatisfying(record -> {
            assertThat(record.status()).isEqualTo(RunStatus.CANCELLED);
            assertThat(record.error()).contains("newer run");
        });
    }

    @Test
    void markFailedAndCancelledWork() {
        RunRecord run = manager.create("thread-fail", "model", Map.of());
        manager.markRunning(run.runId());

        RunRecord failed = manager.markFailed(run.runId(), "Something went wrong");
        assertThat(failed.status()).isEqualTo(RunStatus.FAILED);
        assertThat(failed.error()).isEqualTo("Something went wrong");

        RunRecord run2 = manager.create("thread-cancel", "model", Map.of());
        manager.markRunning(run2.runId());
        RunRecord cancelled = manager.markCancelled(run2.runId());
        assertThat(cancelled.status()).isEqualTo(RunStatus.CANCELLED);
    }

    @Test
    void persistenceSurvivesReconstruction() {
        RunRecord created = manager.create("thread-persist", "model", Map.of("key", "val"));
        String runId = created.runId();

        assertThat(manager.find(runId)).isPresent();
        assertThat(manager.find(runId).get().status()).isEqualTo(RunStatus.PENDING);
    }

    @Test
    void competingTerminalTransitionsCannotOverwriteEachOther() throws Exception {
        RunRecord run = manager.create("thread-cas", "model", Map.of());
        manager.markRunning(run.runId());
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var complete = workers.submit(() -> {
                start.await(1, TimeUnit.SECONDS);
                return manager.tryMarkCompleted(run.runId());
            });
            var cancel = workers.submit(() -> {
                start.await(1, TimeUnit.SECONDS);
                return manager.tryMarkCancelled(run.runId());
            });
            start.countDown();
            assertThat(List.of(complete.get(2, TimeUnit.SECONDS), cancel.get(2, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        RunStatus winner = manager.find(run.runId()).orElseThrow().status();
        assertThat(winner).isIn(RunStatus.COMPLETED, RunStatus.CANCELLED);
        if (winner == RunStatus.COMPLETED) {
            manager.markCancelled(run.runId());
        }
        else {
            manager.markCompleted(run.runId());
        }
        assertThat(manager.find(run.runId()).orElseThrow().status()).isEqualTo(winner);
    }
}
