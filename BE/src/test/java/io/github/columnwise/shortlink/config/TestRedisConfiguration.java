package io.github.columnwise.shortlink.config;

import io.github.columnwise.shortlink.domain.model.ShortUrl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestRedisConfiguration {

    @Bean
    public RedisTemplate<String, ShortUrl> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    public RedisTemplate<String, Object> objectRedisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }
}