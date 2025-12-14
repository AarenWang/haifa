package me.wrj.concurrent.virtualthread;

import java.io.*;
import java.net.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class SocketVThreadBench {

    // ========= 参数 =========
    static final int PORT = 18080;
    static final int CLIENTS = 5_000;        // 并发客户端数
    static final int REQUESTS_PER_CLIENT = 5;
    static final int SERVER_POOL_SIZE = 800; // 平台线程池大小
    static final int SERVER_DELAY_MS = 200;   // 模拟下游延迟
    static final int CLIENT_TIMEOUT_MS = 5_000;

    public static void main(String[] args) throws Exception {
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("CLIENTS=" + CLIENTS +
                ", REQ/CLIENT=" + REQUESTS_PER_CLIENT +
                ", SERVER_DELAY_MS=" + SERVER_DELAY_MS);

        // 1️⃣ 平台线程 Server
        System.out.println("\n=== Platform Threads Server ===");
        runScenario(false);

        // 2️⃣ 虚拟线程 Server
        System.out.println("\n=== Virtual Threads Server ===");
        runScenario(true);
    }

    // ================== 场景执行 ==================
    static void runScenario(boolean virtualThreads) throws Exception {
        ExecutorService serverExecutor =
                virtualThreads
                        ? Executors.newVirtualThreadPerTaskExecutor()
                        : Executors.newFixedThreadPool(SERVER_POOL_SIZE);

        Server server = new Server(serverExecutor);
        Thread serverThread = new Thread(server, "server");
        serverThread.start();

        // 等 server 启动
        Thread.sleep(500);

        long start = System.nanoTime();
        runClients();
        long end = System.nanoTime();

        double seconds = (end - start) / 1e9;
        long totalRequests = (long) CLIENTS * REQUESTS_PER_CLIENT;

        System.out.printf(
                "Total requests: %d, time: %.2fs, throughput: %.0f req/s%n",
                totalRequests,
                seconds,
                totalRequests / seconds
        );

        server.stop();
        serverExecutor.shutdown();
    }

    // ================== Client ==================
    static void runClients() throws Exception {
    try (var clients = Executors.newVirtualThreadPerTaskExecutor()) {
        CountDownLatch latch = new CountDownLatch(CLIENTS);

        for (int i = 0; i < CLIENTS; i++) {
            clients.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("127.0.0.1", PORT), CLIENT_TIMEOUT_MS);
                    socket.setSoTimeout(CLIENT_TIMEOUT_MS);

                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    for (int j = 0; j < REQUESTS_PER_CLIENT; j++) {
                        out.write("ping\n");
                        out.flush();
                        in.readLine();
                    }
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        
       }
    }


    // ================== Server ==================
    static class Server implements Runnable {
        private final ExecutorService executor;
        private volatile boolean running = true;
        private ServerSocket serverSocket;

        Server(ExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public void run() {
            try (ServerSocket ss = new ServerSocket(PORT)) {
                this.serverSocket = ss;
                while (running) {
                    Socket socket = ss.accept(); // 阻塞 accept
                    executor.submit(() -> handle(socket));
                }
            } catch (IOException ignored) {}
        }

        void handle(Socket socket) {
            try (
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    BufferedWriter out = new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream()))
            ) {
                String line;
                while ((line = in.readLine()) != null) {
                    // 模拟下游 I/O 阻塞
                    Thread.sleep(SERVER_DELAY_MS);
                    out.write("pong\n");
                    out.flush();
                }
            } catch (Exception ignored) {}
        }

        void stop() throws IOException {
            running = false;
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
}

