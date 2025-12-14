package me.wrj.concurrent.virtualthread;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class VThreadBench {

    // ====== 你可以调这些参数 ======
    static final int CONCURRENCY = 20_000;     // 并发“请求数”
    static final int POOL_SIZE   = 200;        // 平台线程池大小（常见：200~400）
    static final int BLOCK_MS    = 50;         // 每个请求阻塞等待 50ms（模拟下游/DB/HTTP）
    static final int CPU_SPIN_US = 150;        // 每个请求附带一点点CPU（可设为0）
    static final Duration WARMUP = Duration.ofSeconds(3);
    static final Duration RUN    = Duration.ofSeconds(8);

    public static void main(String[] args) throws Exception {
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("CONCURRENCY=" + CONCURRENCY + ", POOL_SIZE=" + POOL_SIZE +
                ", BLOCK_MS=" + BLOCK_MS + ", CPU_SPIN_US=" + CPU_SPIN_US);

        // 平台线程（固定线程池）
        try (ExecutorService platform = Executors.newFixedThreadPool(POOL_SIZE)) {
            runOnce("PlatformThreads(FixedPool)", platform);
        }

        // 虚拟线程（每任务一个）
        try (ExecutorService vthreads = Executors.newVirtualThreadPerTaskExecutor()) {
            runOnce("VirtualThreads(PerTask)", vthreads);
        }
    }

    static void runOnce(String name, ExecutorService exec) throws Exception {
        // warmup
        bench(name + " [warmup]", exec, WARMUP);

        // actual
        bench(name, exec, RUN);
        System.out.println();
    }

    static void bench(String name, ExecutorService exec, Duration duration) throws Exception {
        final LongAdder completed = new LongAdder();
        final List<Long> latNanos = new CopyOnWriteArrayList<>();

        long endAt = System.nanoTime() + duration.toNanos();

        // 用门闩保证“同时起跑”
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>(CONCURRENCY);

        for (int i = 0; i < CONCURRENCY; i++) {
            futures.add(exec.submit(() -> {
                try {
                    start.await();
                    long t0 = System.nanoTime();
                    while (System.nanoTime() < endAt) {
                        doBlockingWork();
                        completed.increment();
                    }
                    long t1 = System.nanoTime();
                    latNanos.add(t1 - t0); // 粗略：该worker总时间，用于观测调度抖动
                } catch (InterruptedException ignored) {}
            }));
        }

        long tStart = System.nanoTime();
        start.countDown();

        for (Future<?> f : futures) f.get();
        long tEnd = System.nanoTime();

        double seconds = (tEnd - tStart) / 1e9;
        long ops = completed.sum();
        double opsPerSec = ops / seconds;

        // 简单统计：每个worker循环期间的“完成次数”是均匀的，这里只打印吞吐
        System.out.printf("%-30s  ops=%d  time=%.2fs  throughput=%.0f ops/s%n",
                name, ops, seconds, opsPerSec);
    }

    static void doBlockingWork() throws InterruptedException {
        // 1) 模拟下游阻塞：对虚拟线程来说会“挂起”并释放 carrier thread
        Thread.sleep(BLOCK_MS);

        // 2) 模拟一点点CPU（让结果更贴近真实请求）
        if (CPU_SPIN_US > 0) {
            long until = System.nanoTime() + CPU_SPIN_US * 1_000L;
            long x = 0;
            while (System.nanoTime() < until) x ^= 0x9e3779b97f4a7c15L;
            if (x == 42) System.out.print(""); // 防止JIT过度优化
        }
    }
}
