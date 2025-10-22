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

/**
 * Redis 기반 캐시 어댑터 구현체
 *
 * <p>ShortUrl 데이터를 Redis에 캐싱하여 성능을 향상시키는 어댑터입니다.
 * TTL 설정을 통해 자동 만료 처리를 지원하며, 예외 상황에 대한 강고한 처리를 제공합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>ShortUrl 데이터 캐싱 및 조회</li>
 *   <li>TTL 기반 자동 만료 처리</li>
 *   <li>캐시 비워 지기 및 만료 시간 조정</li>
 *   <li>예외 상황 대응 및 로깅</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCacheAdapter implements CachePort {
    
    private final RedisTemplate<String, ShortUrl> redisTemplate;
    private final RedisProperties redisProperties;
    
    /**
     * 단축 코드로 캐시된 ShortUrl을 조회합니다.
     *
     * <p>캐시 히트 시 데이터베이스 접근 없이 빠른 응답을 제공합니다.
     * 예외 발생 시 빈 Optional을 반환하여 안전하게 처리합니다.</p>
     *
     * @param code 조회할 단축 코드
     * @return 캐시된 ShortUrl (Optional)
     */
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
    
    /**
     * ShortUrl을 캐시에 저장합니다.
     *
     * <p>설정된 TTL을 사용하여 자동 만료 처리를 적용합니다.
     * 저장 실패 시 로깅만 하고 예외를 전파하지 않습니다.</p>
     *
     * @param shortUrl 캐시에 저장할 ShortUrl
     */
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
    
    /**
     * 단축 코드에 해당하는 캐시 엔트리를 삭제합니다.
     *
     * @param code 삭제할 단축 코드
     */
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
    
    /**
     * 단축 코드에 해당하는 캐시 엔트리의 만료 시간을 설정합니다.
     *
     * @param code 대상 단축 코드
     * @param seconds 만료 시간 (초 단위, 양수여야 함)
     */
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
    
    /**
     * 단축 코드로부터 Redis 캐시 키를 생성합니다.
     *
     * @param code 단축 코드
     * @return Redis 캐시 키 (프리픽스 + 코드)
     */
    private String getCacheKey(String code) {
        return redisProperties.getCache().getKeyPrefix() + code;
    }
}
