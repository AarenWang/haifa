package org.wrj.haifa.zookeeper.lock;

import java.util.concurrent.TimeUnit;

/**
 * Created by wangrenjun on 2017/4/12.
 */
public interface DistributedLock {

    /**获取锁，如果没有得到就等待*/
    public  void acquire()  throws Exception;

    /**
     * 获取锁，直到超时
     * @param key 锁对象
     * @param time 超时时间
     * @param unit time参数的单位
     * @return是否获取到锁
     * @throws Exception
     */
    public  boolean acquire (String key,long time, TimeUnit unit)  throws Exception;

    /**
     * 释放锁
     * @param key 锁对象
     * @throws Exception
     */
    public  void release(String key)  throws Exception;
}
