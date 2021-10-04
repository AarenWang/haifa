package me.wrj.concurrent.jmm;

/**
 * Created by wangrenjun on 2018/1/20.
 */
public class VolatileExample {

    int  x = 0;
    volatile boolean flag = false;

    /**
     * 写线程
     */
    public void writer(){
        x = 1;  // 1
        flag = true; // 2
    }

    /**
     * 读线程
     */
    public void reader(){
        if(flag){  //3
            System.out.println(x);// 4
        }
    }
}
