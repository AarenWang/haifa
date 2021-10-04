package me.wrj.concurrent.app.threadpool;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wangrenjun on 2018/1/3.
 */
public class ThreadPoolExecutorTest {

    private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) {

        for(int i = 0; i < 10;i++)
        executorService.submit(() -> {
            System.out.println(Thread.currentThread().getName() +", Begin at "+ LocalDateTime.now());
            try {
                Thread.sleep(3000L);
                if(Math.floor((Math.random() * 5)) ==3){
                    throw  new InterruptedException("random = 3");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        });

        executorService.shutdown();


        System.out.println(Thread.currentThread().getName() +", End at "+ LocalDateTime.now());


    }
}
