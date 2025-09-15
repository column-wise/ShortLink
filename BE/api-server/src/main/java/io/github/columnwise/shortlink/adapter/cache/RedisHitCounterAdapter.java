package io.github.columnwise.shortlink.adapter.cache;

import io.github.columnwise.shortlink.application.port.out.UrlHitCounterPort;
import io.github.columnwise.shortlink.config.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 URL 조회 수 카운터 어댑터 구현체
 *
 * <p>Redis의 원자적 연산을 활용하여 동시성 안전한 URL 조회 수 추적을 제공합니다.
 * 고성능 실시간 샤운터로 사용되며, 예외 상황에 대한 강고한 처리를 제공합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>원자적 조회 수 증가 연산</li>
 *   <li>실시간 조회 수 조회</li>
 *   <li>카운터 초기화 및 리셋</li>
 *   <li>예외 상황 안전 처리</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHitCounterAdapter implements UrlHitCounterPort {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisProperties redisProperties;
    
    /**
     * 단축 코드에 대한 조회 수를 원자적으로 증가시킵니다.
     *
     * <p>Redis의 INCR 명령을 사용하여 동시성 안전한 증가 연산을 수행합니다.
     * 증가 실패 시 로깅만 하고 예외를 전파하지 않습니다.</p>
     *
     * @param code 대상 단축 코드
     */
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
    
    /**
     * 단축 코드에 대한 현재 조회 수를 반환합니다.
     *
     * <p>조회 실패 또는 예외 발생 시 0을 반환하여 안전하게 처리합니다.
     * 숫자 형식 오류 시에도 0을 반환합니다.</p>
     *
     * @param code 대상 단축 코드
     * @return 현재 조회 수 (최소 0)
     */
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
    
    /**
     * 단축 코드에 대한 조회 수를 초기화합니다.
     *
     * <p>Redis에서 해당 키를 완전히 삭제하여 다음 증가 시 1부터 시작합니다.</p>
     *
     * @param code 대상 단축 코드
     */
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
    
    /**
     * 단축 코드로부터 Redis 조회 수 키를 생성합니다.
     *
     * @param code 단축 코드
     * @return Redis 조회 수 키 (프리픽스 + 코드)
     */
    private String getHitCountKey(String code) {
        return redisProperties.getHitCounter().getKeyPrefix() + code;
    }
}
