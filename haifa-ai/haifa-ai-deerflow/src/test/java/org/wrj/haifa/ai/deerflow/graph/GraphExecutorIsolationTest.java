package org.wrj.haifa.ai.deerflow.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.GraphExecutorProperties;

class GraphExecutorIsolationTest {

    @Test
    void coordinatorCanWaitForModelWithoutSelfStarvation() throws Exception {
        assertNoStarvation(1, 1);
        assertNoStarvation(2, 3);
        assertNoStarvation(2, 4);
    }

    private static void assertNoStarvation(int coreSize, int runCount) throws Exception {
        GraphExecutionManager manager = manager(coreSize, coreSize, 8, coreSize, coreSize, 8, 2, 2, 8);
        try {
            CountDownLatch entered = new CountDownLatch(Math.min(coreSize, runCount));
            CompletableFuture<?>[] runs = new CompletableFuture[runCount];
            for (int i = 0; i < runs.length; i++) {
                String runId = "run-" + i;
                runs[i] = CompletableFuture.runAsync(() -> {
                    entered.countDown();
                    String thread = CompletableFuture.supplyAsync(() -> Thread.currentThread().getName(),
                            manager.getModelExecutor(runId)).join();
                    assertThat(thread).startsWith("test-model-");
                }, manager.getCoordinatorExecutor(runId));
            }
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            CompletableFuture.allOf(runs).get(2, TimeUnit.SECONDS);
            assertThat(manager.coordinatorStatus().queueSize()).isZero();
            assertThat(manager.modelStatus().queueSize()).isZero();
        }
        finally {
            manager.destroy();
        }
    }

    static GraphExecutionManager manager(int coordinatorCore, int coordinatorMax, int coordinatorQueue,
            int modelCore, int modelMax, int modelQueue, int toolCore, int toolMax, int toolQueue) throws Exception {
        GraphExecutorProperties properties = new GraphExecutorProperties();
        properties.setCoordinatorCorePoolSize(coordinatorCore);
        properties.setCoordinatorMaxPoolSize(coordinatorMax);
        properties.setCoordinatorQueueCapacity(coordinatorQueue);
        properties.setCoordinatorThreadNamePrefix("test-coordinator-");
        properties.setCorePoolSize(modelCore);
        properties.setMaxPoolSize(modelMax);
        properties.setQueueCapacity(modelQueue);
        properties.setThreadNamePrefix("test-model-");
        properties.setToolCorePoolSize(toolCore);
        properties.setToolMaxPoolSize(toolMax);
        properties.setToolQueueCapacity(toolQueue);
        properties.setToolThreadNamePrefix("test-tool-");
        GraphExecutionManager manager = new GraphExecutionManager(properties);
        manager.afterPropertiesSet();
        return manager;
    }
}
