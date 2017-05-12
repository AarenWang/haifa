package me.wrj.haifa.zookeeper.lock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

/**
 * Created by wangrenjun on 2017/4/26.
 */
public abstract  class ZKBaseLock implements Watcher {

    protected static final String DEFAULT_LOCK_ZNODE_NAME      = "/ZK_LOCK";

    protected static final int    DEFAULT_TIME_OUT_MILLSECONDS = 3000;

    protected String              connectString;

    protected ZooKeeper zk;

    private ZKBaseLock(){

    }

    public ZKBaseLock(String connectString){
        this.connectString = connectString;
        if (zk == null) {
            try {
                zk = new ZooKeeper(connectString, DEFAULT_TIME_OUT_MILLSECONDS, this);
                Stat stat = zk.exists(DEFAULT_LOCK_ZNODE_NAME, false);
                if (stat == null) {
                    zk.create(DEFAULT_LOCK_ZNODE_NAME, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void process(WatchedEvent event) {

    }
}
