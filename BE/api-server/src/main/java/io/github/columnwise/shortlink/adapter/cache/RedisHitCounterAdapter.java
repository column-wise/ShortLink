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
        if (code == null || code.trim().isEmpty()) {
            log.warn("Cannot increment hit count for null or empty code");
            return;
        }
        
        try {
            String key = getHitCountKey(code);
            Long newCount = redisTemplate.opsForValue().increment(key);
            log.debug("Successfully incremented hit count for code: {} to {}", code, newCount);
        } catch (Exception e) {
            log.warn("Failed to increment hit count for code: {}", code, e);
        }
    }
    
    @Override
    public long getHitCount(String code) {
        if (code == null || code.trim().isEmpty()) {
            log.warn("Cannot get hit count for null or empty code");
            return 0L;
        }
        
        try {
            String key = getHitCountKey(code);
            String count = redisTemplate.opsForValue().get(key);
            long result = count != null ? Long.parseLong(count) : 0L;
            log.debug("Retrieved hit count for code: {} is {}", code, result);
            return result;
        } catch (NumberFormatException e) {
            log.warn("Invalid hit count format for code: {}, returning 0", code, e);
            return 0L;
        } catch (Exception e) {
            log.warn("Failed to get hit count for code: {}", code, e);
            return 0L;
        }
    }
    
    @Override
    public void resetHitCount(String code) {
        if (code == null || code.trim().isEmpty()) {
            log.warn("Cannot reset hit count for null or empty code");
            return;
        }
        
        try {
            String key = getHitCountKey(code);
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Successfully reset hit count for code: {}", code);
            } else {
                log.debug("No hit count entry found to reset for code: {}", code);
            }
        } catch (Exception e) {
            log.warn("Failed to reset hit count for code: {}", code, e);
        }
    }
    
    private String getHitCountKey(String code) {
        return redisProperties.getHitCounter().getKeyPrefix() + code;
    }
}
