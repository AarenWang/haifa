package org.wrj.haifa.guava;

import com.google.common.util.concurrent.RateLimiter;

public class RateLimiterTest {

    public static void main(String[] args) {
        RateLimiter rateLimiter = RateLimiter.create(1000);

        for(int i =0; i < 100; i++) {
            MyThread myThread = new MyThread(rateLimiter);
            Thread  thread = new Thread(myThread);
            thread.start();;
        }
    }
}


class MyThread implements Runnable{

    RateLimiter rateLimiter;

    public MyThread(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void run() {
        while (true){
            double acquire = rateLimiter.acquire(100);
            System.out.printf("thread %s acquire %.3f \n",Thread.currentThread().getName(),acquire);

            try {
                Thread.sleep(new Double(Math.random() * 100).longValue());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
