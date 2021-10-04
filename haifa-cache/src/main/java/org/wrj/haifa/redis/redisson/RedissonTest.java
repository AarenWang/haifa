package org.wrj.haifa.redis.redisson;

import org.redisson.Redisson;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * Created by wangrenjun on 2017/9/20.
 */
public class RedissonTest {

    public static void main(String[] args) {

        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonClient redisson = Redisson.create(config);
        RList<String> list = redisson.getList("key1");
        list.addAsync("ONE");



    }
}
