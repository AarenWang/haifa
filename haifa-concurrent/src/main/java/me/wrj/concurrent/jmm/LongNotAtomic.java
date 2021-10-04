package me.wrj.concurrent.jmm;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wangrenjun on 2018/1/14.
 */
public class LongNotAtomic implements  Runnable{

    public volatile long  a = 0;

    public AtomicInteger ai = new AtomicInteger(0);

    int LOOP_COUNT = 200000;


    public  void increment(){
         a++;
        ai.getAndIncrement();
    }

    @Override
    public void run() {

        for(int i = 0; i < LOOP_COUNT;i++){
            increment();
        }
        System.out.println("Done");

    }

    public static void main(String[] args) {

        int THREAD_COUNT = 5;
        LongNotAtomic longNotAtomic = new LongNotAtomic();


        for(int i = 0; i < THREAD_COUNT;i++){

            new Thread(longNotAtomic).start();
        }

        try {
            Thread.sleep(2000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("a == 1000000? "+ (longNotAtomic.a == 1000000)+",a = "+longNotAtomic.a);
        System.out.println("ai == 1000000? "+ (longNotAtomic.ai.get() == 1000000)+",a = "+longNotAtomic.ai.get());

    }



}
