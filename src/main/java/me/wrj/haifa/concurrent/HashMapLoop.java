package me.wrj.haifa.concurrent;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wangrenjun on 2017/5/24.
 */
public class HashMapLoop {

    private static final int SIZE   = 100000;
    private List<String>     myList = new ArrayList<String>(SIZE);

    public static void main(String[] args) {

        HashMapLoop loop = new HashMapLoop();
        loop.initMyList();
        loop.concurrentLoop();

    }

    private void concurrentLoop() {
        ExecutorService executorService = Executors.newFixedThreadPool(200);
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < myList.size(); i++) {

            executorService.execute(new MyThread(map, i + "", myList.get(i)));
        }

        executorService.shutdown();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    private void initMyList() {
        for (int i = 0; i < SIZE; i++) {
            myList.add(i + "");
        }
    }

}

class MyThread implements Runnable {

    private String              key;

    private String              value;

    private Map<String, String> map;

    public MyThread(Map<String, String> map, String key, String value){
        this.map = map;
        this.key = key;
        this.value = value;
    }

    @Override
    public void run() {

        map.put(key, value);
        long l = System.currentTimeMillis();
        try {
            Random random = new Random();
            long millSeconds = random.nextInt(20);
            Thread.sleep(millSeconds);
            System.out.printf("Thread name = %s,put key=%s,value=%s sleep %s millseconds \n",
                              Thread.currentThread().getName(), key, value, millSeconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

        }

    }
}
