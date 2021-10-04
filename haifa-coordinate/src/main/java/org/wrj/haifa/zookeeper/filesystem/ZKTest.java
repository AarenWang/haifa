package org.wrj.haifa.zookeeper.filesystem;


import org.apache.zookeeper.*;

import java.io.IOException;

/**
 * Created by wangrenjun on 2017/4/6.
 */
public class ZKTest{

    public static void main(String[] args) throws InterruptedException, IOException,KeeperException {
        ZooKeeper zooKeeper = new ZooKeeper("127.0.0.1:2181", Integer.MAX_VALUE, new Watcher() {

            @Override
            public void process(WatchedEvent watchedEvent) {
                System.out.println("event type:" + watchedEvent.getType() + ",path=" + watchedEvent.getPath()
                                   + ",State=" + watchedEvent.getState());

            }
        });

        while (true){
            if(zooKeeper.exists("/haifa",false) == null){
                zooKeeper.create("/haifa","haifa".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

            }
            for(int i = 0 ; i < 10; i++){
                try {
                    zooKeeper.create("/haifa/path"+i,("data"+i).getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                    System.out.println("/haifa/path"+i+" created!!!");
                    Thread.sleep(1000);
                } catch (KeeperException e) {
                    e.printStackTrace();
                }

            }

            Thread.sleep(2000);
            for(int i = 0 ; i < 10; i++){
                try {
                    zooKeeper.delete("/haifa/path"+i,0);
                    System.out.println("/haifa/path"+i+" delete!!!");
                    Thread.sleep(1000);
                } catch (KeeperException e) {
                    e.printStackTrace();
                }

            }



        }

        //zooKeeper.close();

    }
}
