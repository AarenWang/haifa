package org.wrj.haifa.process;

import java.time.LocalDateTime;

public class HookTest {

    public static void main(String[] args) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    System.out.println("current thread is " + Thread.currentThread().getName() + ",Time is " + LocalDateTime.now());
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    System.out.println(" ShutdownHook " + Thread.currentThread().getName() + ",Time is " + LocalDateTime.now());
                    System.out.println("我就是不退出 你怎么办");
                    try {
                        Thread.sleep(3000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }));

    }
}
