package io.github.columnwise.shortlink.adapter.cache;

import io.github.columnwise.shortlink.application.port.out.CachePort;
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
    
    private static final String CACHE_KEY_PREFIX = "shorturl:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Optional<ShortUrl> findByCode(String code) {
        try {
            String key = getCacheKey(code);
            ShortUrl cached = (ShortUrl) redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(cached);
        } catch (Exception e) {
            log.warn("Failed to get from cache for code: {}", code, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void save(ShortUrl shortUrl) {
        try {
            String key = getCacheKey(shortUrl.code());
            redisTemplate.opsForValue().set(key, shortUrl, DEFAULT_TTL);
            log.debug("Cached ShortUrl for code: {}", shortUrl.code());
        } catch (Exception e) {
            log.warn("Failed to cache ShortUrl for code: {}", shortUrl.code(), e);
        }
    }
    
    @Override
    public void delete(String code) {
        try {
            String key = getCacheKey(code);
            redisTemplate.delete(key);
            log.debug("Deleted cache for code: {}", code);
        } catch (Exception e) {
            log.warn("Failed to delete cache for code: {}", code, e);
        }
    }
    
    @Override
    public void setExpiration(String code, long seconds) {
        try {
            String key = getCacheKey(code);
            redisTemplate.expire(key, Duration.ofSeconds(seconds));
            log.debug("Set expiration for code: {} to {} seconds", code, seconds);
        } catch (Exception e) {
            log.warn("Failed to set expiration for code: {}", code, e);
        }
    }
    
    private String getCacheKey(String code) {
        return CACHE_KEY_PREFIX + code;
    }
}
