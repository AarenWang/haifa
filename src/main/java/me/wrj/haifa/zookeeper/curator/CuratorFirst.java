package me.wrj.haifa.zookeeper.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * Created by wangrenjun on 2017/5/4.
 */
public class CuratorFirst {

    public static void main(String[] args) {

        String zookeeperConnectionString = "127.0.0.1:2181";
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
        client.start();

        try {
            client.create().forPath("/curator","Hello Curator!".getBytes());
            byte[] bytes = client.getData().forPath("/curator");
            System.out.printf("/curator data is %s",new String(bytes));



        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
