package org.wrj.haifa.tracing;

/**
 * Created by wangrenjun on 2017/8/16.
 */
public class TracingDemo {

    public static void main(String[] args) {
        ThreadLocal<String> tl = new ThreadLocal<>();
        tl.set(Thread.currentThread().getName()+"10001");

        TracingService tracingService = new TracingService();
        tracingService.service1();
    }
}
