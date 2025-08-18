package io.github.columnwise.shortlink.adapter.cache;

import io.github.columnwise.shortlink.application.port.out.CachePort;
import io.github.columnwise.shortlink.config.RedisProperties;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCacheAdapter implements CachePort {
    
    private final RedisTemplate<String, ShortUrl> redisTemplate;
    private final RedisProperties redisProperties;
    
    @Override
    public Optional<ShortUrl> findByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            log.warn("Cannot find cache entry for null or empty code");
            return Optional.empty();
        }
        
        try {
            String key = getCacheKey(code);
            ShortUrl cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for code: {}", code);
            } else {
                log.debug("Cache miss for code: {}", code);
            }
            return Optional.ofNullable(cached);
        } catch (Exception e) {
            log.warn("Failed to get from cache for code: {}", code, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void save(ShortUrl shortUrl) {
        if (shortUrl == null || shortUrl.code() == null || shortUrl.code().trim().isEmpty()) {
            log.warn("Cannot save null ShortUrl or ShortUrl with null/empty code");
            return;
        }
        
        try {
            String key = getCacheKey(shortUrl.code());
            redisTemplate.opsForValue().set(key, shortUrl, redisProperties.getCache().getDefaultTtl());
            log.debug("Successfully cached ShortUrl for code: {} with TTL: {}", 
                shortUrl.code(), redisProperties.getCache().getDefaultTtl());
        } catch (Exception e) {
            log.warn("Failed to cache ShortUrl for code: {}", shortUrl.code(), e);
        }
    }
    
    @Override
    public void delete(String code) {
        if (code == null || code.trim().isEmpty()) {
            log.warn("Cannot delete cache for null or empty code");
            return;
        }
        
        try {
            String key = getCacheKey(code);
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Successfully deleted cache for code: {}", code);
            } else {
                log.debug("No cache entry found to delete for code: {}", code);
            }
        } catch (Exception e) {
            log.warn("Failed to delete cache for code: {}", code, e);
        }
    }
    
    @Override
    public void setExpiration(String code, long seconds) {
        if (code == null || code.trim().isEmpty()) {
            log.warn("Cannot set expiration for null or empty code");
            return;
        }
        if (seconds <= 0) {
            log.warn("Invalid expiration time: {} seconds. Must be positive", seconds);
            return;
        }
        
        try {
            String key = getCacheKey(code);
            Boolean expired = redisTemplate.expire(key, Duration.ofSeconds(seconds));
            if (Boolean.TRUE.equals(expired)) {
                log.debug("Successfully set expiration for code: {} to {} seconds", code, seconds);
            } else {
                log.debug("Failed to set expiration - key may not exist for code: {}", code);
            }
        } catch (Exception e) {
            log.warn("Failed to set expiration for code: {}", code, e);
        }
    }
    
    private String getCacheKey(String code) {
        return redisProperties.getCache().getKeyPrefix() + code;
    }
}
