package me.wrj.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class SpringbootApplicationTests {

	@Autowired
	private RedisTemplate redisTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void testRedis() {
		redisTemplate.opsForValue().set("test", "test");
		System.out.println(redisTemplate.opsForValue().get("test"));
	}


}
