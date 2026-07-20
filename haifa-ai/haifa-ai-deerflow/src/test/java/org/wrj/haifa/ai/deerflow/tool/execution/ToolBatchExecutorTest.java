package org.wrj.haifa.ai.deerflow.tool.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.GraphExecutorProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.run.RunCancellationToken;

class ToolBatchExecutorTest {

    private GraphExecutionManager manager;
    private ToolBatchExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        GraphExecutorProperties properties = new GraphExecutorProperties();
        properties.setCoordinatorCorePoolSize(1);
        properties.setCoordinatorMaxPoolSize(1);
        properties.setCoordinatorQueueCapacity(4);
        properties.setCorePoolSize(1);
        properties.setMaxPoolSize(1);
        properties.setQueueCapacity(4);
        properties.setToolCorePoolSize(4);
        properties.setToolMaxPoolSize(4);
        properties.setToolQueueCapacity(16);
        manager = new GraphExecutionManager(properties);
        manager.afterPropertiesSet();
        executor = new ToolBatchExecutor(manager, properties);
    }

    @AfterEach
    void tearDown() {
        manager.destroy();
    }

    @Test
    void parallelSafeOverlapsAndResultsKeepRequestOrder() throws Exception {
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        var request = new ToolBatchRequest<>("run-parallel", List.of(
                task("first", ToolConcurrencyMode.PARALLEL_SAFE, "", () -> awaitAndReturn(entered, release, "A")),
                task("second", ToolConcurrencyMode.PARALLEL_SAFE, "", () -> awaitAndReturn(entered, release, "B"))),
                2, new RunCancellationToken("run-parallel"));
        var future = executor.execute(request);
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        assertThat(future.get(1, TimeUnit.SECONDS).items())
                .extracting(ToolBatchItemResult::callId).containsExactly("first", "second");
    }

    @Test
    void defaultSerialAndSameResourceNeverOverlapButDifferentResourcesCan() throws Exception {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        List<ToolExecutionTask<String>> serial = List.of(
                observed("a", ToolConcurrencyMode.SERIAL_PER_RUN, "", active, maxActive),
                observed("b", ToolConcurrencyMode.SERIAL_PER_RUN, "", active, maxActive));
        executor.execute(new ToolBatchRequest<>("run-serial", serial, 4,
                new RunCancellationToken("run-serial"))).get(2, TimeUnit.SECONDS);
        assertThat(maxActive).hasValue(1);

        active.set(0);
        maxActive.set(0);
        executor.execute(new ToolBatchRequest<>("run-same-resource", List.of(
                        observed("same-1", ToolConcurrencyMode.SERIAL_PER_RESOURCE, "shared", active, maxActive),
                        observed("same-2", ToolConcurrencyMode.SERIAL_PER_RESOURCE, "shared", active, maxActive)),
                4, new RunCancellationToken("run-same-resource"))).get(2, TimeUnit.SECONDS);
        assertThat(maxActive).hasValue(1);

        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        var resourceFuture = executor.execute(new ToolBatchRequest<>("run-resource", List.of(
                task("r1", ToolConcurrencyMode.SERIAL_PER_RESOURCE, "one", () -> awaitAndReturn(entered, release, "1")),
                task("r2", ToolConcurrencyMode.SERIAL_PER_RESOURCE, "two", () -> awaitAndReturn(entered, release, "2"))),
                4, new RunCancellationToken("run-resource")));
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        resourceFuture.get(1, TimeUnit.SECONDS);
    }

    @Test
    void cancellationPreventsQueuedTaskFromStarting() throws Exception {
        RunCancellationToken token = new RunCancellationToken("run-cancel");
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger secondStarts = new AtomicInteger();
        var future = executor.execute(new ToolBatchRequest<>("run-cancel", List.of(
                task("first", ToolConcurrencyMode.SERIAL_PER_RUN, "", () -> awaitAndReturn(firstStarted, release, "1")),
                task("second", ToolConcurrencyMode.SERIAL_PER_RUN, "", () -> {
                    secondStarts.incrementAndGet();
                    return "2";
                })), 1, token));
        assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();
        token.cancel("TEST_CANCEL");
        release.countDown();
        ToolBatchResult<String> result = future.get(1, TimeUnit.SECONDS);
        assertThat(secondStarts).hasValue(0);
        assertThat(result.items().get(1).status()).isEqualTo(ToolBatchItemResult.Status.CANCELLED);
    }

    @Test
    void perRunLimitCapsParallelismAndFailureDoesNotCancelSafeSibling() throws Exception {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        var result = executor.execute(new ToolBatchRequest<>("run-limit", List.of(
                observed("one", ToolConcurrencyMode.PARALLEL_SAFE, "", active, maxActive),
                task("failed", ToolConcurrencyMode.PARALLEL_SAFE, "", () -> {
                    throw new IllegalStateException("expected");
                }),
                observed("three", ToolConcurrencyMode.PARALLEL_SAFE, "", active, maxActive)),
                1, new RunCancellationToken("run-limit"))).get(2, TimeUnit.SECONDS);

        assertThat(maxActive).hasValue(1);
        assertThat(result.items()).extracting(ToolBatchItemResult::status).containsExactly(
                ToolBatchItemResult.Status.SUCCESS,
                ToolBatchItemResult.Status.FAILED,
                ToolBatchItemResult.Status.SUCCESS);
    }

    @Test
    void cancellationMarksAlreadyRunningNonCooperativeToolAsLateCompletion() throws Exception {
        RunCancellationToken token = new RunCancellationToken("run-late");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var future = executor.execute(new ToolBatchRequest<>("run-late", List.of(
                task("running", ToolConcurrencyMode.PARALLEL_SAFE, "",
                        () -> awaitAndReturn(entered, release, "done"))), 1, token));

        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        token.cancel("USER_CANCELLED");
        release.countDown();

        ToolBatchItemResult<String> item = future.get(1, TimeUnit.SECONDS).items().getFirst();
        assertThat(item.status()).isEqualTo(ToolBatchItemResult.Status.SUCCESS);
        assertThat(item.lateCompletion()).isTrue();
    }

    private static ToolExecutionTask<String> observed(String id, ToolConcurrencyMode mode, String key,
            AtomicInteger active, AtomicInteger maxActive) {
        return task(id, mode, key, () -> {
            int now = active.incrementAndGet();
            maxActive.accumulateAndGet(now, Math::max);
            Thread.yield();
            active.decrementAndGet();
            return id;
        });
    }

    private static ToolExecutionTask<String> task(String id, ToolConcurrencyMode mode, String key,
            java.util.function.Supplier<String> action) {
        return new ToolExecutionTask<>(id, mode, key, action);
    }

    private static String awaitAndReturn(CountDownLatch entered, CountDownLatch release, String result) {
        entered.countDown();
        try {
            if (!release.await(2, TimeUnit.SECONDS)) {
                throw new AssertionError("release timeout");
            }
            return result;
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }
}
