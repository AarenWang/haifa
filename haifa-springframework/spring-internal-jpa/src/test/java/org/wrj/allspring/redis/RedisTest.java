package org.wrj.allspring.redis;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = {"classpath:spring/jpa/spring-jpainternal.xml"})
public class RedisTest {


    @Autowired
    private RedisTemplate redisTemplate;


    @Test
    public void test(){
        ValueOperations vo = redisTemplate.opsForValue();
        String rawValue = "test_value";
        redisTemplate.opsForValue().set("test_key",rawValue,10L, TimeUnit.SECONDS);
        String value = (String) redisTemplate.opsForValue().get("test_key");
        Assert.assertTrue(rawValue.equals(value));

    }
}
