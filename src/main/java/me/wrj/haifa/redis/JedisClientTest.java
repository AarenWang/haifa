package me.wrj.haifa.redis;

import org.apache.commons.collections.CollectionUtils;
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


    }
}
