package org.wrj.haifa.concurrent;

import org.apache.commons.lang3.RandomUtils;

import java.util.concurrent.*;

public class FutureTask {

    public static void main(String[] args) throws Exception{
        BlockingQueue bq = new ArrayBlockingQueue(5);
        ThreadPoolExecutor executor  = new ThreadPoolExecutor(3,5,1000L, TimeUnit.MICROSECONDS,bq);
        Future<String> futureTask1 = executor.submit(new SpiderTask<String>("baidu"));
        Future<String> futureTask2 = executor.submit(new SpiderTask<String>("sina"));
        Future<String> futureTask3 = executor.submit(new SpiderTask<String>("taobao"));
        Future<String> futureTask4 = executor.submit(new SpiderTask<String>("weibo"));

        System.out.println(futureTask1.get());
        System.out.println(futureTask2.get());
        System.out.println(futureTask3.get());
        System.out.println(futureTask4.get());


        executor.shutdown();

    }
}


class SpiderTask<String> implements Callable{

    private  String name;

    public SpiderTask(String name) {
        this.name = name;
    }

    @Override
    public String call() throws Exception {
        System.out.println("spider task "+name+" begin running");
        int content = RandomUtils.nextInt(5,10);
        return name;
    }
}
