package org.wrj.haifa.zookeeper.lock;

/**
 * Created by wangrenjun on 2017/4/26.
 */
public class ZKReadWriterLock extends ZKBaseLock {


    public ZKReadWriterLock(String connectString) {
        super(connectString);
    }
}
