package org.wrj.allspring.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DataRedisPipelineDemo {

    @Autowired
    private RedisTemplate redisTemplate;

    // inject the template as ListOperations
    private ListOperations<String, String> listOps;

    public void addLink(String userId, URL url) {
        listOps.leftPush(userId, url.toExternalForm());
    }


    public static void main(String[] args) {
        ApplicationContext ac = new ClassPathXmlApplicationContext("spring/jpa/spring-jpainternal.xml");
        RedisTemplate<String, String> template = ac.getBean(RedisTemplate.class);
        template.executePipelined(new RedisCallback<Object>() {

            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                return null;
            }
        });


    }


}
