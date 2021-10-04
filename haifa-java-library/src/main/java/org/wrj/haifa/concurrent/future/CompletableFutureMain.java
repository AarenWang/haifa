package org.wrj.haifa.concurrent.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureMain {

    public static int cal(int para){
        try {
            Thread.sleep(2000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("CompletableFutureMain thread finish");
        return  para * para;
    }
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() ->
            cal(50)).thenAccept(System.out::println);
        //System.out.println(future.get());
        System.out.println("main thread finish");

    }
}
