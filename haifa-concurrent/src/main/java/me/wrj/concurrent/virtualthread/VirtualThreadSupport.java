package me.wrj.concurrent.virtualthread;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility methods that let the demos run on both Java 17 (without official
 * virtual threads) and Java 21+ (with virtual thread support). The class relies
 * on reflection so that it compiles on older Java versions while still being
 * able to opt in to virtual threads when available at runtime.
 */
public final class VirtualThreadSupport {

    private static final Method START_VIRTUAL_THREAD;
    private static final Method OF_VIRTUAL;
    private static final Method BUILDER_START;
    private static final Method BUILDER_UNSTARTED;

    private static final AtomicInteger FALLBACK_COUNTER = new AtomicInteger();

    static {
        START_VIRTUAL_THREAD = findStartVirtualThread();
        OF_VIRTUAL = findOfVirtual();
        BUILDER_START = findBuilderStart();
        BUILDER_UNSTARTED = findBuilderUnstarted();
    }

    private VirtualThreadSupport() {
    }

    public static boolean isSupported() {
        return START_VIRTUAL_THREAD != null || (OF_VIRTUAL != null && (BUILDER_START != null || BUILDER_UNSTARTED != null));
    }

    public static Thread startThread(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        if (START_VIRTUAL_THREAD != null) {
            try {
                return (Thread) START_VIRTUAL_THREAD.invoke(null, runnable);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("unable to start virtual thread", e);
            }
        }
        if (OF_VIRTUAL != null && BUILDER_START != null) {
            try {
                Object builder = OF_VIRTUAL.invoke(null);
                return (Thread) BUILDER_START.invoke(builder, runnable);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("unable to start virtual thread", e);
            }
        }
        Thread fallback = new Thread(runnable, "virtual-thread-fallback-" + FALLBACK_COUNTER.incrementAndGet());
        fallback.start();
        return fallback;
    }

    public static Thread newUnstartedThread(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        if (OF_VIRTUAL != null && BUILDER_UNSTARTED != null) {
            try {
                Object builder = OF_VIRTUAL.invoke(null);
                return (Thread) BUILDER_UNSTARTED.invoke(builder, runnable);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("unable to create virtual thread", e);
            }
        }
        return new Thread(runnable, "virtual-thread-fallback-" + FALLBACK_COUNTER.incrementAndGet());
    }

    public static <T> CompletableFuture<T> task(Callable<T> callable) {
        Objects.requireNonNull(callable, "callable");
        CompletableFuture<T> future = new CompletableFuture<>();
        startThread(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private static Method findStartVirtualThread() {
        try {
            return Thread.class.getMethod("startVirtualThread", Runnable.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findOfVirtual() {
        try {
            return Thread.class.getMethod("ofVirtual");
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findBuilderStart() {
        try {
            Class<?> builderClass = Class.forName("java.lang.Thread$Builder$OfVirtual");
            return builderClass.getMethod("start", Runnable.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findBuilderUnstarted() {
        try {
            Class<?> builderClass = Class.forName("java.lang.Thread$Builder$OfVirtual");
            return builderClass.getMethod("unstarted", Runnable.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return null;
        }
    }
}
