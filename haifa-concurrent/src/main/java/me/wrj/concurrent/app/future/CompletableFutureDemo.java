package me.wrj.concurrent.app.future;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CompletableFutureDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println(" =================  执行无返回结果和有返回结果Future ======================= ");
        testSupplyResult();

        System.out.println(" =================  执行多步骤Future ======================= ");
        testSupplyResult();

        System.out.println(" =================  执行多步骤数据交互Future ======================= ");
        testMultiStepInteractive();

        System.out.println(" =================  执行多步骤数据Compose ======================= ");
        testMultiStepCompose();

    }


    public static void testSupplyResult() throws ExecutionException, InterruptedException {
        Runnable runnable = () ->
                System.out.println("执行无返回结果的异步任务");
        System.out.println(CompletableFuture.runAsync(runnable).get());

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("执行有返回值的异步任务");
            return "Hello World";
        });
        String result = future.get();
        System.out.println(result);
    }

    public static void testMultiStep() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {

            }
            System.out.println("执行结束1！");
            return 5;
        });

        future2.whenComplete(new BiConsumer<Integer, Throwable>() {
                    @Override
                    public void accept(Integer t, Throwable action) {
                        t = t+1;
//                int i = 12 / 0;
                        System.out.println("执行完成2！"+action.getMessage());
                    }
                })
                .exceptionally(new Function<Throwable, Integer>() {
                    @Override
                    public Integer apply(Throwable t) {
                        System.out.println("执行失败3：" + t.getMessage());
                        return null;
                    }
                }).join();
        Integer integer = future2.get();
        System.out.println("=> integer = "+integer);
    }


    public static void testMultiStepInteractive() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            int result = 100;
            System.out.println("一阶段：" + result);
            return result;
        }).thenApply(number -> {
            int result = number * 3;
            System.out.println("二阶段：" + result);
            return result;
        }).thenApply(number -> {
            int result = number * 3;
            System.out.println("三阶段：" + result);
            return result;
        });

        System.out.println("最终结果：" + future.get());

    }


    public static  void testMultiStepCompose() throws ExecutionException, InterruptedException{
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            int number = new Random().nextInt(10);
            System.out.println("第一阶段：" + number);
            return number;
        }).thenCompose(param -> CompletableFuture.supplyAsync(() -> {
            int number = param * 2;
            System.out.println("第二阶段：" + number);
            return number;
        }));
        System.out.println("最终结果: " + future.get());

    }
}
