package org.wrj.haifa.redis;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class PubSubDemo
{
    public static void main( String[] args )
    {
        // 替换成你的reids地址和端口
        String redisIp = "127.0.0.1";
        int reidsPort = 6379;

        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), redisIp, reidsPort);
        System.out.println(String.format("redis pool is starting, redis ip %s, redis port %d", redisIp, reidsPort));

        SubThread subThread = new SubThread(jedisPool);
        subThread.start();

        Publisher publisher = new Publisher(jedisPool);
        publisher.start();
    }
}