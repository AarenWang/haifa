package me.wrj.springboot;

import me.wrj.springboot.entity.Stu;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

import javax.annotation.Resource;

@SpringBootTest
class SpringbootApplicationTests {

	@Resource
	private RedisTemplate<String,Object> redisTemplate;

	@Test
	void contextLoads() {

	}

	@Test
	void testRedis() {

		redisTemplate.opsForValue().set("test", "test");
		System.out.println(redisTemplate.opsForValue().get("test"));

		redisTemplate.getConnectionFactory().getConnection().flushAll();
	}

	@Test
	void  testList(){
		String key = "test.str_list";
		redisTemplate.opsForList().leftPush(key,"one");
		redisTemplate.opsForList().leftPush(key,"two");
		redisTemplate.opsForList().leftPush(key,"three");

		String value = (String) redisTemplate.opsForList().leftPop(key);
		Assert.isTrue(value.equals("three"));

		value = (String) redisTemplate.opsForList().leftPop(key);
		Assert.isTrue(value.equals("two"));

		value = (String) redisTemplate.opsForList().leftPop(key);
		Assert.isTrue(value.equals("one"));

	}

	@Test
	void  testStuList(){
		String key = "test.stu_list";
		redisTemplate.opsForList().leftPush(key,new Stu("Jack",22));
		redisTemplate.opsForList().leftPush(key,new Stu("Mack",23));
		redisTemplate.opsForList().leftPush(key,new Stu("Pony",24));

		var v1= redisTemplate.opsForList().leftPop(key);
		System.out.printf("v1=%s,v1 Type=%s \n", v1, v1.getClass().getName());

//		Stu value = (Stu) redisTemplate.opsForList().leftPop(key);
//		Assert.isTrue(value.getAge() == 24);
//
//		value = (Stu) redisTemplate.opsForList().leftPop(key);
//		Assert.isTrue(value.getAge() == 23);
//
//		value = (Stu) redisTemplate.opsForList().leftPop(key);
//		Assert.isTrue(value.getAge() == 22);
	}

	@Test
	void testSet(){
		String key = "test.stu_set";
		redisTemplate.opsForSet().add(key,new Stu("Jack",22));
		redisTemplate.opsForSet().add(key,new Stu("Jack",22));
		redisTemplate.opsForSet().add(key,new Stu("Mack",23));
		Assert.isTrue(redisTemplate.opsForSet().isMember(key,new Stu("Jack",22)));
		Assert.isTrue(!redisTemplate.opsForSet().isMember(key,new Stu("Jack",23)));


		System.out.println("set size="+redisTemplate.opsForSet().size(key));

	}


}
