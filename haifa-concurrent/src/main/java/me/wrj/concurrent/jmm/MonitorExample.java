package me.wrj.concurrent.jmm;

/**
 * Created by wangrenjun on 2018/1/28.
 */


public class MonitorExample {

    int a = 0;

    public synchronized void writer() { // 1
        a++; // 2
    } // 3

    public synchronized void reader() { // 4
        int i = a; // 5
        // ......
    }// 6
}
