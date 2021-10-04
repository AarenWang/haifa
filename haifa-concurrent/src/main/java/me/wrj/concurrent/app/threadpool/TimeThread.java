package me.wrj.concurrent.app.threadpool;

import java.time.LocalDateTime;

/**
 * Created by wangrenjun on 2018/1/3.
 */
class  TimeThread implements Runnable{

    @Override
    public void run() {
        System.out.println("current time is "+ LocalDateTime.now()+",thread name is "+Thread.currentThread().getName());
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
