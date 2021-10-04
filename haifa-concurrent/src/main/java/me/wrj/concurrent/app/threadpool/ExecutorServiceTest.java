package me.wrj.concurrent.app.threadpool;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wangrenjun on 2018/1/3.
 */
public class ExecutorServiceTest {


    public static void main(String[] args) {
        //ExecutorService executorService = Executors.newFixedThreadPool(5);
        ExecutorService executorService = Executors.newCachedThreadPool();


        TimeThread timeTask = new TimeThread();
        for(int  i=0; i < 10000; i++){
            executorService.submit(timeTask);
        }


        executorService.shutdown();
    }
}

