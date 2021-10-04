package org.wrj.haifa.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Created by wangrenjun on 2017/5/8.
 */
public class JedisPoolTest {

    public static void main(String[] args) {
        JedisPool pool = new JedisPool("127.0.0.1",6379);
        Jedis jedis = pool.getResource();
        jedis.set("foo","bar");
        System.out.println(jedis.get("foo"));

        for (int i = 0; i < 10; i++) {
            Jedis aJedis = pool.getResource();
            String pong = aJedis.ping();
            System.out.printf("i = %d, pong = %s \n" ,i,pong);

        }
    }
}
