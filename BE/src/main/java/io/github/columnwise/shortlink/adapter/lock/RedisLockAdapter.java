package io.github.columnwise.shortlink.adapter.lock;

import io.github.columnwise.shortlink.application.port.out.DistributedLockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisLockAdapter implements DistributedLockPort {
    
    private static final String LOCK_KEY_PREFIX = "lock:";
    private static final String UNLOCK_SCRIPT = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ThreadLocal<String> lockValues = new ThreadLocal<>();
    
    @Override
    public boolean tryLock(String key, Duration expiration) {
        try {
            String lockKey = getLockKey(key);
            String lockValue = UUID.randomUUID().toString();
            
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expiration);
            
            if (Boolean.TRUE.equals(success)) {
                lockValues.set(lockValue);
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
            String lockValue = lockValues.get();
            
            if (lockValue == null) {
                log.warn("No lock value found for key: {}", key);
                return;
            }
            
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);
            
            Long result = redisTemplate.execute(script, 
                    Collections.singletonList(lockKey), lockValue);
            
            if (result != null && result == 1L) {
                log.debug("Released lock for key: {}", key);
            } else {
                log.warn("Failed to release lock for key: {} - lock may have expired", key);
            }
            
            lockValues.remove();
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
        return LOCK_KEY_PREFIX + key;
    }
}
