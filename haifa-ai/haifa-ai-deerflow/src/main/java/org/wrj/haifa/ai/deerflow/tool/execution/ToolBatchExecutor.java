package org.wrj.haifa.ai.deerflow.tool.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.GraphExecutorProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.run.RunCancellationToken;

/** Graph-independent, bounded and order-preserving tool batch scheduler. */
@Component
public class ToolBatchExecutor {

    private final GraphExecutionManager executionManager;
    private final Semaphore globalPermits;

    public ToolBatchExecutor(GraphExecutionManager executionManager, GraphExecutorProperties properties) {
        this.executionManager = executionManager;
        this.globalPermits = new Semaphore(Math.max(1, properties.getToolMaxPoolSize()), true);
    }

    public <T> CompletableFuture<ToolBatchResult<T>> execute(ToolBatchRequest<T> request) {
        Semaphore runPermits = new Semaphore(request.maxConcurrency(), true);
        List<CompletableFuture<ToolBatchItemResult<T>>> futures = new ArrayList<>();
        Map<String, CompletableFuture<?>> resourceTails = new HashMap<>();
        CompletableFuture<?> barrier = CompletableFuture.completedFuture(null);

        for (ToolExecutionTask<T> task : request.tasks()) {
            CompletableFuture<?> dependency;
            switch (task.concurrencyMode()) {
                case PARALLEL_SAFE -> dependency = barrier;
                case SERIAL_PER_RESOURCE -> dependency = CompletableFuture.allOf(
                        asVoid(barrier), asVoid(resourceTails.get(task.resourceKey())));
                case SERIAL_PER_RUN, EXCLUSIVE -> dependency = CompletableFuture.allOf(
                        futures.stream().map(ToolBatchExecutor::asVoid).toArray(CompletableFuture[]::new));
                default -> dependency = barrier;
            }

            CompletableFuture<ToolBatchItemResult<T>> future = dependency.thenCompose(ignored ->
                    submit(request.runId(), task, request.cancellationToken(), runPermits));
            futures.add(future);
            if (task.concurrencyMode() == ToolConcurrencyMode.SERIAL_PER_RESOURCE) {
                resourceTails.put(task.resourceKey(), future);
            }
            if (task.concurrencyMode() == ToolConcurrencyMode.SERIAL_PER_RUN
                    || task.concurrencyMode() == ToolConcurrencyMode.EXCLUSIVE) {
                barrier = future;
                resourceTails.clear();
            }
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        return all.thenApply(ignored -> new ToolBatchResult<>(futures.stream().map(CompletableFuture::join).toList(),
                request.cancellationToken().isCancelled()));
    }

    private <T> CompletableFuture<ToolBatchItemResult<T>> submit(String runId, ToolExecutionTask<T> task,
            RunCancellationToken token, Semaphore runPermits) {
        if (token.isCancelled()) {
            return CompletableFuture.completedFuture(ToolBatchItemResult.cancelled(task.callId()));
        }
        try {
            return CompletableFuture.supplyAsync(() -> executeTask(task, token, runPermits),
                    executionManager.getToolExecutor(runId));
        }
        catch (RuntimeException ex) {
            return CompletableFuture.completedFuture(ToolBatchItemResult.failed(task.callId(), ex));
        }
    }

    private <T> ToolBatchItemResult<T> executeTask(ToolExecutionTask<T> task, RunCancellationToken token,
            Semaphore runPermits) {
        boolean runAcquired = false;
        boolean globalAcquired = false;
        try {
            while (!token.isCancelled() && !(runAcquired = runPermits.tryAcquire(50, TimeUnit.MILLISECONDS))) {
                // Timed acquisition makes queued work observe cooperative cancellation.
            }
            if (!runAcquired || token.isCancelled()) {
                return ToolBatchItemResult.cancelled(task.callId());
            }
            while (!token.isCancelled() && !(globalAcquired = globalPermits.tryAcquire(50, TimeUnit.MILLISECONDS))) {
                // See above.
            }
            if (!globalAcquired || token.isCancelled()) {
                return ToolBatchItemResult.cancelled(task.callId());
            }
            T value = task.action().get();
            return ToolBatchItemResult.success(task.callId(), value, token.isCancelled());
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ToolBatchItemResult.cancelled(task.callId());
        }
        catch (Throwable ex) {
            return ToolBatchItemResult.failed(task.callId(), ex);
        }
        finally {
            if (globalAcquired) {
                globalPermits.release();
            }
            if (runAcquired) {
                runPermits.release();
            }
        }
    }

    private static CompletableFuture<Void> asVoid(CompletableFuture<?> future) {
        return future == null ? CompletableFuture.completedFuture(null) : future.thenApply(ignored -> null);
    }
}
