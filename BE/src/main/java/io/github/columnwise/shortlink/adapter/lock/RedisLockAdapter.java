package io.github.columnwise.shortlink.adapter.lock;

import io.github.columnwise.shortlink.application.port.out.DistributedLockPort;
import io.github.columnwise.shortlink.config.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisLockAdapter implements DistributedLockPort {
    
    private static final String UNLOCK_SCRIPT = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";
    
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisProperties redisProperties;
    private final ThreadLocal<Map<String, String>> lockValues = new ThreadLocal<>();
    
    @Override
    public boolean tryLock(String key, Duration expiration) {
        try {
            String lockKey = getLockKey(key);
            String lockValue = UUID.randomUUID().toString();
            
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expiration);
            
            if (Boolean.TRUE.equals(success)) {
                Map<String, String> tokens = lockValues.get();
                if (tokens == null) {
                    tokens = new HashMap<>();
                    lockValues.set(tokens);
                }
                tokens.put(key, lockValue);
                log.debug("Acquired lock for key: {}", key);
                return true;
            }
            
            log.debug("Failed to acquire lock for key: {}", key);
            return false;
        } catch (Exception e) {
            log.warn("Failed to try lock for key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public void unlock(String key) {
        try {
            String lockKey = getLockKey(key);
            Map<String, String> tokens = lockValues.get();
            
            if (tokens == null || !tokens.containsKey(key)) {
                log.warn("No lock value found for key: {}", key);
                return;
            }
            
            String lockValue = tokens.get(key);
            
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);
            
            Long result = redisTemplate.execute(script, 
                    Collections.singletonList(lockKey), lockValue);
            
            if (result != null && result == 1L) {
                log.debug("Released lock for key: {}", key);
                tokens.remove(key);
                if (tokens.isEmpty()) {
                    lockValues.remove();
                }
            } else {
                log.warn("Failed to release lock for key: {} - lock may have expired", key);
            }
        } catch (Exception e) {
            log.warn("Failed to unlock for key: {}", key, e);
        }
    }
    
    @Override
    public boolean isLocked(String key) {
        try {
            String lockKey = getLockKey(key);
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.warn("Failed to check lock status for key: {}", key, e);
            return false;
        }
    }
    
    private String getLockKey(String key) {
        return redisProperties.getLock().getKeyPrefix() + key;
    }
}
