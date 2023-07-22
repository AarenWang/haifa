package me.wrj.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Objects;

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

		Stu value = (Stu) redisTemplate.opsForList().leftPop(key);
		Assert.isTrue(value.getAge() == 24);

		value = (Stu) redisTemplate.opsForList().leftPop(key);
		Assert.isTrue(value.getAge() == 23);

		value = (Stu) redisTemplate.opsForList().leftPop(key);
		Assert.isTrue(value.getAge() == 22);
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


	static class Stu /*implements Serializable */ {
		String name;

		int age;

		public Stu(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}


		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Stu stu = (Stu) o;
			return age == stu.age && Objects.equals(name, stu.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, age);
		}
	}

}
