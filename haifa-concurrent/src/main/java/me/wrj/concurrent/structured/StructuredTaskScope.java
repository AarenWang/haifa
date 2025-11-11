package me.wrj.concurrent.structured;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A lightweight educational alternative to the JDK's preview Structured Concurrency API.
 * It uses a dedicated executor and keeps track of child tasks. When the scope is closed all
 * tasks are cancelled and the executor is shut down. The class is intentionally simple and
 * meant for demonstration purposes.
 */
public class StructuredTaskScope<T> implements AutoCloseable {

    private final ExecutorService executor;
    private final List<CompletableFuture<T>> tasks = new ArrayList<>();

    public StructuredTaskScope() {
        this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    public StructuredTaskScope(ThreadFactory threadFactory) {
        this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), threadFactory));
    }

    public StructuredTaskScope(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<T> fork(Supplier<T> supplier) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Throwable ex) {
                throw new CompletionException(ex);
            }
        }, executor);
        synchronized (tasks) {
            tasks.add(future);
        }
        return future;
    }

    public List<T> joinAll() {
        List<CompletableFuture<T>> snapshot;
        synchronized (tasks) {
            snapshot = new ArrayList<>(tasks);
        }
        List<T> results = new ArrayList<>(snapshot.size());
        for (CompletableFuture<T> task : snapshot) {
            results.add(task.join());
        }
        return results;
    }

    public void cancelAll() {
        synchronized (tasks) {
            tasks.forEach(future -> future.cancel(true));
        }
    }

    @Override
    public void close() {
        cancelAll();
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    public static StructuredTaskScope<Void> timed(Duration timeout) {
        ExecutorService executor = Executors.newCachedThreadPool();
        return new TimedScope(executor, timeout);
    }

    private static class TimedScope extends StructuredTaskScope<Void> {
        private final ExecutorService executor;
        private final Duration timeout;

        private TimedScope(ExecutorService executor, Duration timeout) {
            super(executor);
            this.executor = executor;
            this.timeout = timeout;
        }

        @Override
        public void close() {
            cancelAll();
            executor.shutdown();
            try {
                executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
