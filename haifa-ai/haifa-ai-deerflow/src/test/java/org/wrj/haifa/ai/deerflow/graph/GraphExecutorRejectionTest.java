package org.wrj.haifa.ai.deerflow.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class GraphExecutorRejectionTest {

    @Test
    void rejectionContainsRunAndPoolState() throws Exception {
        GraphExecutionManager manager = GraphExecutorIsolationTest.manager(1, 1, 1, 1, 1, 1, 1, 1, 1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            manager.getToolExecutor("run-reject").execute(() -> await(release));
            manager.getToolExecutor("run-reject").execute(() -> { });
            assertThatThrownBy(() -> manager.getToolExecutor("run-reject").execute(() -> { }))
                    .isInstanceOf(GraphExecutorRejectedException.class)
                    .hasMessageContaining("run-reject")
                    .hasMessageContaining("executor=tool");
            assertThat(manager.toolStatus().rejectedCount()).isEqualTo(1);
        }
        finally {
            release.countDown();
            manager.destroy();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
