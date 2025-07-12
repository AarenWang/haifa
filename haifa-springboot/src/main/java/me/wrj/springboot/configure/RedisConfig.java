package me.wrj.springboot.configure;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.wrj.springboot.entity.Stu;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Stu> redisTemplate2(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Stu> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 设置Key的序列化器
        template.setKeySerializer(new StringRedisSerializer());
        // 设置Value的序列化器
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // hash的key序列化方式也是采用String类型
        template.setHashKeySerializer(new StringRedisSerializer());
        //hash的value也是采用jackson类型
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());



        template.afterPropertiesSet();
        return template;
    }


    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Enable default typing to include class information in JSON
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

}
