package org.wrj.haifa.tracing;

/**
 * Created by wangrenjun on 2017/8/16.
 */
public class TracingUtil {

    public  static void   doTrace(){
        ThreadLocal tl = new ThreadLocal();
        tl.set(Thread.currentThread().getName());
    }
}
