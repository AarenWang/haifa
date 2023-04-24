package me.wrj.concurrent;

import java.util.concurrent.locks.LockSupport;

public class LockSupportDemo {

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(() -> {
            System.out.println("do something start");
            LockSupport.park();
            System.out.println("do something end");
        });

        thread.start();
        Thread.sleep(3000);

        System.out.println("给子线程thread增加一个许可");
        LockSupport.unpark(thread);


        Thread thread2 = new Thread(() -> {
            System.out.println("thread2 do something start");
            System.out.println("thread2 子线程thread给自己增加一个许可");
            LockSupport.unpark(Thread.currentThread());
            LockSupport.park();
            System.out.println("thread2 do something end");
        });

        thread2.start();

    }

}
