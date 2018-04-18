package org.wrj.haifa.zookeeper.lock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

/**
 * Created by wangrenjun on 2017/4/26.
 */
public class ZKLock extends ZKBaseLock {



    public ZKLock(String connectString){
        super(connectString);
    }

    public boolean lock(String key) throws Exception {

        Stat stat = zk.exists(DEFAULT_LOCK_ZNODE_NAME + key, false);
        if (stat != null) {
            // 目前不支持锁等待，后序需要加上一定的锁等待，在等待时间内，如果锁释放了 也可以成功获取
            return false;
        }
        zk.create(DEFAULT_LOCK_ZNODE_NAME + key, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

        return true;
    }

    public void unLock(String key) throws Exception {

        Stat stat = zk.exists(DEFAULT_LOCK_ZNODE_NAME + key, false);
        if (stat == null) {
            throw new IllegalStateException("no lock node exists");
        }
        zk.delete(DEFAULT_LOCK_ZNODE_NAME + key, -1);

    }

}
