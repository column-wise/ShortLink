package io.github.columnwise.shortlink.adapter.redis;

import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReader;
import io.github.columnwise.shortlink.domain.service.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class ClusterRedisStatisticsReaderAdapter implements RedisStatisticsReader {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Set<String> findAccessCountKeys(LocalDate date) {
        // KEYS 명령 대신 SET으로 관리되는 코드 목록 사용
        String codesSetKey = RedisKeyManager.getAccessCodesSetKey(date);
        Set<Object> codes = redisTemplate.opsForSet().members(codesSetKey);
        
        if (codes == null || codes.isEmpty()) {
            log.debug("No access codes found for date: {}", date);
            return Collections.emptySet();
        }
        
        // 코드 목록을 실제 키로 변환
        return codes.stream()
                .map(Object::toString)
                .map(code -> RedisKeyManager.getAccessCountKey(code, date))
                .collect(Collectors.toSet());
    }

    @Override
    public Long getAccessCount(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Invalid access count value for key {}: {}", key, value);
            return null;
        }
    }

    @Override
    public Set<String> findDailyStatisticsKeys(LocalDate date) {
        // KEYS 명령 대신 SET으로 관리되는 코드 목록 사용
        String codesSetKey = RedisKeyManager.getDailyCodesSetKey(date);
        Set<Object> codes = redisTemplate.opsForSet().members(codesSetKey);
        
        if (codes == null || codes.isEmpty()) {
            log.debug("No daily statistics codes found for date: {}", date);
            return Collections.emptySet();
        }
        
        // 코드 목록을 실제 키로 변환
        return codes.stream()
                .map(Object::toString)
                .map(code -> RedisKeyManager.getDailyStatsKey(code, date))
                .collect(Collectors.toSet());
    }

    @Override
    public Map<Object, Object> getDailyStatistics(String key) {
        Map<Object, Object> stats = redisTemplate.opsForHash().entries(key);
        return stats != null ? stats : Collections.emptyMap();
    }
}