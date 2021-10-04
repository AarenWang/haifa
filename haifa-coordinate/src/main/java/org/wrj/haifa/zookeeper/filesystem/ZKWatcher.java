package org.wrj.haifa.zookeeper.filesystem;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.List;

/**
 * Created by wangrenjun on 2017/4/22.
 */
public class ZKWatcher implements Watcher {

    static ZooKeeper zooKeeper = null;

    @Override
    public void process(WatchedEvent event) {

        System.out.println("event type:" + event.getType() + ",path:" + event.getPath());
        if (event.getType() == Event.EventType.NodeChildrenChanged) {
            try {
                List<String> subList = zooKeeper.getChildren("/haifa", true);
                System.out.println("/haifa node hava " + subList.size() + " child node,The Child NODE is "
                                   + StringUtils.join(subList, ","));
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws KeeperException, InterruptedException {
        try {
            zooKeeper = new ZooKeeper("127.0.0.1:2181", Integer.MAX_VALUE, new ZKWatcher());
            zooKeeper.getChildren("/haifa", new ZKWatcher());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
