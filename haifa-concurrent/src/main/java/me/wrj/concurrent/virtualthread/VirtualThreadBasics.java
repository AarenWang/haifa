package me.wrj.concurrent.virtualthread;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

/**
 * Basic usage: start a handful of lightweight tasks using virtual threads when
 * available. The fallback uses platform threads but keeps the same API.
 */
public class VirtualThreadBasics {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("virtual threads supported -> " + VirtualThreadSupport.isSupported());

        CountDownLatch latch = new CountDownLatch(3);
        start("alpha", latch);
        start("beta", latch);
        start("gamma", latch);

        latch.await();
    }

    private static void start(String name, CountDownLatch latch) {
        VirtualThreadSupport.startThread(() -> {
            Instant start = Instant.now();
            try {
                Thread.sleep(100);
                System.out.printf("task %s executed on %s%n", name, Thread.currentThread());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.printf("task %s latency %s%n", name, Duration.between(start, Instant.now()));
                latch.countDown();
            }
        });
    }
}
