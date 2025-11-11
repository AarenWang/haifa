package me.wrj.concurrent.structured;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demonstrates the basic lifecycle of a structured task scope. Child tasks are
 * created inside the scope and automatically cancelled when the scope is
 * closed.
 */
public class StructuredConcurrencyBasics {

    public static void main(String[] args) {
        try (StructuredTaskScope<String> scope = new StructuredTaskScope<>()) {
            scope.fork(() -> fetchProfile("alice"));
            scope.fork(() -> fetchProfile("bob"));
            scope.fork(() -> fetchProfile("charlie"));

            List<String> profiles = scope.joinAll();
            profiles.forEach(profile -> System.out.println("profile -> " + profile));
        }
    }

    private static String fetchProfile(String user) {
        simulateLatency();
        return "profile{" + user + "}@" + Instant.now();
    }

    private static void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(50, 150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
