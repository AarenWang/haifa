package me.wrj.concurrent.structured;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Advanced example that aggregates results from heterogeneous services and
 * keeps track of both successes and failures.
 */
public class StructuredResilientAggregator {

    public static void main(String[] args) {
        List<Outcome<String>> outcomes = new ArrayList<>();

        try (StructuredTaskScope<String> scope = new StructuredTaskScope<>()) {
            CompletableFuture<String> payments = scope.fork(() -> callService("payments", 120, false));
            CompletableFuture<String> inventory = scope.fork(() -> callService("inventory", 210, true));
            CompletableFuture<String> shipping = scope.fork(() -> callService("shipping", 90, false));

            outcomes.add(await("payments", payments));
            outcomes.add(await("inventory", inventory));
            outcomes.add(await("shipping", shipping));
        }

        outcomes.forEach(outcome -> {
            if (outcome.error() == null) {
                System.out.printf("service %s result -> %s%n", outcome.name(), outcome.value());
            } else {
                System.out.printf("service %s failed: %s%n", outcome.name(), outcome.error());
            }
        });
    }

    private static Outcome<String> await(String name, CompletableFuture<String> future) {
        try {
            return new Outcome<>(name, future.join(), null);
        } catch (CompletionException ex) {
            return new Outcome<>(name, null, ex.getCause());
        }
    }

    private static String callService(String name, int baseLatency, boolean flaky) {
        simulateLatency(baseLatency);
        if (flaky && ThreadLocalRandom.current().nextBoolean()) {
            throw new IllegalStateException(name + " unavailable");
        }
        return name + "-" + System.nanoTime();
    }

    private static void simulateLatency(int baseLatency) {
        try {
            int latency = (int) (baseLatency + ThreadLocalRandom.current().nextDouble(120));
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        }
    }

    record Outcome<T>(String name, T value, Throwable error) {
    }
}
