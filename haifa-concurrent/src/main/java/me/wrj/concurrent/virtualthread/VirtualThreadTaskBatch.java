package me.wrj.concurrent.virtualthread;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Intermediate example: submit a batch of independent operations and combine
 * their results using {@link CompletableFuture}. Each task uses a virtual
 * thread when the runtime supports it.
 */
public class VirtualThreadTaskBatch {

    public static void main(String[] args) {
        List<CompletableFuture<String>> futures = List.of(
                VirtualThreadSupport.task(() -> fetch("inventory")),
                VirtualThreadSupport.task(() -> fetch("pricing")),
                VirtualThreadSupport.task(() -> fetch("recommendation"))
        );

        futures.stream()
                .map(CompletableFuture::join)
                .forEach(result -> System.out.println("result -> " + result));
    }

    private static String fetch(String name) throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextInt(80, 200));
        return name + "@" + Instant.now();
    }
}
