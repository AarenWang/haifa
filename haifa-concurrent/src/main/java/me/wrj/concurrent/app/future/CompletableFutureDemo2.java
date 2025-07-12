package me.wrj.concurrent.app.future;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CompletableFutureDemo2 {

    public static void main(String[] args) throws Exception {

        join();
    }

    public static void join(){
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println(LocalDateTime.now()+" Future 1 is running...sleep 3000");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 5;
        });
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println(LocalDateTime.now()+" Future 2 is running...sleep 3000");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 10;
        });
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        futures.add(future1);
        futures.add(future2);
        futures.stream().map(CompletableFuture::join).collect(Collectors.toList())
                .forEach(result -> System.out.println(LocalDateTime.now()+" Result: " + result));
    }

    public static void allOf(){
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println(LocalDateTime.now()+" Future 1 is running...sleep 3000");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 5;
        });
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println(LocalDateTime.now()+" Future 2 is running...sleep 3000");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 10;
        });

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(future1, future2);
        combinedFuture.join();

        try {
            int result1 = future1.get();
            int result2 = future2.get();
            System.out.println(LocalDateTime.now()+" Result 1: " + result1);
            System.out.println(LocalDateTime.now()+" Result 2: " + result2);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

}
