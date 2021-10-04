package me.wrj.concurrent.jmm;

/**
 * Created by wangrenjun on 2018/1/14.
 */
public class FinalFieldExample {

    final int x;
    int y;
    static FinalFieldExample f;

    public FinalFieldExample() {
        x = 3;
        y = 4;
    }

    //写线程
    static void writer() {
        f = new FinalFieldExample();
    }

    //读线程
    static void reader() {
        if (f != null) {
            int i = f.x;  // 保证i读取到一定是3
            int j = f.y;  // 可能返回的是0
        }
    }

}
