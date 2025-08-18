package io.github.columnwise.shortlink.adapter.cache;

import io.github.columnwise.shortlink.application.port.out.UrlHitCounterPort;
import io.github.columnwise.shortlink.config.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHitCounterAdapter implements UrlHitCounterPort {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisProperties redisProperties;
    
    @Override
    public void incrementHitCount(String code) {
        try {
            String key = getHitCountKey(code);
            redisTemplate.opsForValue().increment(key);
            log.debug("Incremented hit count for code: {}", code);
        } catch (Exception e) {
            log.warn("Failed to increment hit count for code: {}", code, e);
        }
    }
    
    @Override
    public long getHitCount(String code) {
        try {
            String key = getHitCountKey(code);
            String count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count) : 0L;
        } catch (Exception e) {
            log.warn("Failed to get hit count for code: {}", code, e);
            return 0L;
        }
    }
    
    @Override
    public void resetHitCount(String code) {
        try {
            String key = getHitCountKey(code);
            redisTemplate.delete(key);
            log.debug("Reset hit count for code: {}", code);
        } catch (Exception e) {
            log.warn("Failed to reset hit count for code: {}", code, e);
        }
    }
    
    private String getHitCountKey(String code) {
        return redisProperties.getHitCounter().getKeyPrefix() + code;
    }
}
