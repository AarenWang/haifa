package me.wrj.concurrent.virtualthread;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Advanced demo: dispatch a burst of lightweight tasks while bounding
 * concurrency with a semaphore. Virtual threads are ideal for such scenarios
 * because they release the carrier thread whenever they block.
 */
public class VirtualThreadRequestDispatcher {

    private static final int REQUESTS = 50;
    private static final int PARALLELISM = 8;

    public static void main(String[] args) throws InterruptedException {
        Semaphore semaphore = new Semaphore(PARALLELISM);
        CountDownLatch latch = new CountDownLatch(REQUESTS);

        for (int i = 0; i < REQUESTS; i++) {
            final int requestId = i;
            Thread thread = VirtualThreadSupport.newUnstartedThread(() -> {
                try {
                    semaphore.acquire();
                    String result = handleRequest(requestId);
                    System.out.printf("request %d processed -> %s%n", requestId, result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                    latch.countDown();
                }
            });
            thread.start();
        }

        latch.await();
    }

    private static String handleRequest(int id) throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextInt(20, 120));
        return "response{" + id + "}@" + Instant.now();
    }
}
