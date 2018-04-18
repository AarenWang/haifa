package org.wrj.haifa.redis;

import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * Created by wangrenjun on 2017/4/8.
 */
public class JedisClientTest {

    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost");
        jedis.set("foo", "bar");

        String value = jedis.get("foo");
        System.out.println(value);

        jedis.rpush("mylist","a","b","c","d");
        List<String> mylist = jedis.lrange("mylist",0,-1);
        System.out.println(StringUtils.join(mylist,","));


        jedis.hset("user-1001","name","Jaack");
        jedis.hset("user-1001","age","20");
        jedis.hset("user-1001","gender","male");
        System.out.println(jedis.hgetAll("user-1001"));
        jedis.rpush("mylist","a","b","c");
        System.out.println(jedis.lrange("mylist",0,2));
        System.out.printf("mylist.len=%s",jedis.llen("mylist"));


        jedis.zadd("math_score",70D,"Mao");
        jedis.zadd("math_score",75D,"Jack");
        jedis.zadd("math_score",81D,"Mark");
        jedis.zadd("math_score",63D,"Lucy");
        jedis.zadd("math_score",85D,"Jun");
        jedis.zadd("math_score",91D,"Alex");


        System.out.printf("math_score between 70 and 85 %s",jedis.zrangeByScore("math_score",70,85));






    }
}
