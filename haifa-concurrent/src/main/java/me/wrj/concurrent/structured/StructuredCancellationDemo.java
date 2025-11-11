package me.wrj.concurrent.structured;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Illustrates cooperative cancellation. The first successful result wins and
 * the remaining tasks are cancelled.
 */
public class StructuredCancellationDemo {

    public static void main(String[] args) {
        try (StructuredTaskScope<String> scope = new StructuredTaskScope<>()) {
            CompletableFuture<String> primary = scope.fork(() -> fetchFromService("primary", 120));
            CompletableFuture<String> secondary = scope.fork(() -> fetchFromService("secondary", 320));
            CompletableFuture<String> slowBackup = scope.fork(() -> fetchFromService("backup", 750));

            String winner = CompletableFuture.anyOf(primary, secondary, slowBackup)
                    .thenApply(String.class::cast)
                    .join();

            System.out.println("winner -> " + winner);
            scope.cancelAll();
        }
    }

    private static String fetchFromService(String name, int baseLatency) {
        simulateLatency(baseLatency);
        return name + "@" + System.nanoTime();
    }

    private static void simulateLatency(int baseLatency) {
        try {
            int latency = (int) (baseLatency + ThreadLocalRandom.current().nextDouble(100));
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("task interrupted", e);
        }
    }
}
