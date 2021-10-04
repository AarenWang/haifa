package org.wrj.allspring.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.net.URL;

public class DataRedisPipelineDemo {



    // inject the template as ListOperations
    @Resource(name="redisTemplate")
    private ListOperations<String, String> listOps;

    public void addLink(String userId, URL url) {
        listOps.leftPush(userId, url.toExternalForm());
    }


    public static void main(String[] args) {
        ApplicationContext ac = new ClassPathXmlApplicationContext("spring-data-redis/spring-data-redis.xml");
        RedisTemplate<String, String> template = ac.getBean(RedisTemplate.class);
        template.executePipelined(new RedisCallback<Object>() {

            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                return null;
            }
        });
    }


}
