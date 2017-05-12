package me.wrj.haifa.zookeeper.lock;

import org.junit.Test;

/**
 * Created by wangrenjun on 2017/4/26.
 */
public class ZKDistributedLockTest {


    @Test
    public void testLock(){
        String hostString = "127.0.0.1:2181";
        ZKDistributedLock lock = new ZKDistributedLock(hostString);
        String key = "prod-01";
        try {
            boolean flag1 = lock.lock(key);
            boolean flag2 = lock.lock(key); // 不支持重入
            System.out.println("flag1=" + flag1 + ",flag2=" + flag2);
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                lock.unLock(key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
