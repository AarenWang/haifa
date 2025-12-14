package me.wrj.concurrent.virtualthread;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class SocketVThreadBench2 {

    // ===== 参数 =====
    static final int PORT = 18080;
    static final int CLIENTS = 5_000;
    static final int REQUESTS_PER_CLIENT = 5;

    static final int SERVER_POOL_SIZE = 800;      // 平台线程池大小
    static final int SERVER_DELAY_MS = 200;       // 模拟下游延迟
    static final int CLIENT_TIMEOUT_MS = 10_000;

    // 每个请求延迟采样（纳秒）
    static final long[] latNanos = new long[CLIENTS * REQUESTS_PER_CLIENT];
    static final AtomicInteger latIdx = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("CLIENTS=" + CLIENTS + ", REQ/CLIENT=" + REQUESTS_PER_CLIENT +
                ", SERVER_DELAY_MS=" + SERVER_DELAY_MS + ", SERVER_POOL_SIZE=" + SERVER_POOL_SIZE);

        System.out.println("\n=== Platform Threads Server ===");
        runScenario(false);

        // 重置采样
        Arrays.fill(latNanos, 0);
        latIdx.set(0);

        System.out.println("\n=== Virtual Threads Server ===");
        runScenario(true);
    }

    static void runScenario(boolean vthreads) throws Exception {
        ExecutorService serverExec;
        ThreadPoolExecutor tpe = null;

        if (vthreads) {
            serverExec = Executors.newVirtualThreadPerTaskExecutor();
        } else {
            tpe = new ThreadPoolExecutor(
                    SERVER_POOL_SIZE, SERVER_POOL_SIZE,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    r -> {
                        Thread t = new Thread(r);
                        t.setName("plat-" + t.getId());
                        t.setDaemon(true);
                        return t;
                    }
            );
            serverExec = tpe;
        }

        Server server = new Server(serverExec);
        Thread serverThread = new Thread(server, "server");
        serverThread.start();
        Thread.sleep(400);

        // 平台线程场景：每秒打印活跃线程/队列
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        if (tpe != null) {
            ThreadPoolExecutor finalTpe = tpe;
            monitor.scheduleAtFixedRate(() -> {
                System.out.printf("[monitor] active=%d queue=%d completed=%d%n",
                        finalTpe.getActiveCount(),
                        finalTpe.getQueue().size(),
                        finalTpe.getCompletedTaskCount());
            }, 0, 1, TimeUnit.SECONDS);
        }

        long totalReq = (long) CLIENTS * REQUESTS_PER_CLIENT;

        long t0 = System.nanoTime();
        runClients();          // 真正请求在这里发生
        long t1 = System.nanoTime();

        monitor.shutdownNow();
        server.stop();
        serverExec.shutdown();

        double sec = (t1 - t0) / 1e9;
        double tps = totalReq / sec;

        System.out.printf("Total requests: %d, time: %.2fs, throughput: %.0f req/s%n",
                totalReq, sec, tps);

        printLatencyStats();
    }

    static void runClients() throws Exception {
        // 客户端用虚拟线程把并发打满（否则并发会被线程池限制）
        ExecutorService clients = Executors.newVirtualThreadPerTaskExecutor();
        try {
            CountDownLatch latch = new CountDownLatch(CLIENTS);

            for (int i = 0; i < CLIENTS; i++) {
                clients.submit(() -> {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress("127.0.0.1", PORT), CLIENT_TIMEOUT_MS);
                        socket.setSoTimeout(CLIENT_TIMEOUT_MS);

                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                        for (int j = 0; j < REQUESTS_PER_CLIENT; j++) {
                            long s = System.nanoTime();
                            out.write("ping\n");
                            out.flush();
                            in.readLine();
                            long e = System.nanoTime();

                            int idx = latIdx.getAndIncrement();
                            if (idx < latNanos.length) latNanos[idx] = (e - s);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        } finally {
            clients.shutdown();
        }
    }

    static void printLatencyStats() {
        int n = latIdx.get();
        if (n <= 0) return;

        long[] copy = Arrays.copyOf(latNanos, n);
        Arrays.sort(copy);

        long p50 = percentile(copy, 50);
        long p95 = percentile(copy, 95);
        long p99 = percentile(copy, 99);
        long avg = average(copy);

        System.out.printf("Latency (ms): avg=%.2f p50=%.2f p95=%.2f p99=%.2f  (samples=%d)%n",
                avg / 1e6, p50 / 1e6, p95 / 1e6, p99 / 1e6, n);
    }

    static long percentile(long[] sorted, int p) {
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        if (idx < 0) idx = 0;
        if (idx >= sorted.length) idx = sorted.length - 1;
        return sorted[idx];
    }

    static long average(long[] a) {
        long sum = 0;
        for (long v : a) sum += v;
        return sum / Math.max(1, a.length);
    }

    static class Server implements Runnable {
        private final ExecutorService exec;
        private volatile boolean running = true;
        private ServerSocket ss;

        Server(ExecutorService exec) { this.exec = exec; }

        @Override public void run() {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                this.ss = serverSocket;
                while (running) {
                    Socket s = serverSocket.accept();
                    exec.submit(() -> handle(s));
                }
            } catch (IOException ignored) {}
        }

        void handle(Socket socket) {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
            ) {
                String line;
                while ((line = in.readLine()) != null) {
                    Thread.sleep(SERVER_DELAY_MS);
                    out.write("pong\n");
                    out.flush();
                }
            } catch (Exception ignored) {}
        }

        void stop() throws IOException {
            running = false;
            if (ss != null) ss.close();
        }
    }

    // 小工具
    static final class AtomicInteger {
        private final java.util.concurrent.atomic.AtomicInteger ai;
        AtomicInteger(int v) { ai = new java.util.concurrent.atomic.AtomicInteger(v); }
        int getAndIncrement() { return ai.getAndIncrement(); }
        void set(int v) { ai.set(v); }
        int get() { return ai.get(); }
    }
}

