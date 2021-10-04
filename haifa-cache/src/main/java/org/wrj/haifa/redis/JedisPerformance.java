package org.wrj.haifa.redis;

import redis.clients.jedis.Jedis;

/**
 * Created by wangrenjun on 2017/4/19.
 */
public class JedisPerformance {

    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost");
        long begin = System.currentTimeMillis();
        for(int i = 1; i < 100000; i++){
            jedis.set("k"+i,"v"+i);
        }
        System.out.println("cost "+(System.currentTimeMillis() - begin) +" millsecond(s)");

    }
}
