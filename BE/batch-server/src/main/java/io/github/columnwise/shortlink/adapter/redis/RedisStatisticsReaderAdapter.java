package io.github.columnwise.shortlink.adapter.redis;

import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisStatisticsReaderAdapter implements RedisStatisticsReader {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ACCESS_COUNT_KEY_PREFIX = "url:access:count:";
    private static final String DAILY_STATS_KEY_PREFIX = "url:daily:stats:";

    @Override
    public Set<String> findAccessCountKeys(LocalDate date) {
        // 모든 접근 카운트 키를 조회한 후, 해당 날짜에 해당하는 것만 필터링
        String pattern = ACCESS_COUNT_KEY_PREFIX + "*";
        Set<String> allKeys = redisTemplate.keys(pattern);
        
        if (allKeys == null || allKeys.isEmpty()) {
            return Collections.emptySet();
        }
        
        String targetDatePrefix = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return allKeys.stream()
                .filter(key -> key.contains(targetDatePrefix))
                .collect(java.util.stream.Collectors.toSet());
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
            return null;
        }
    }

    @Override
    public Set<String> findDailyStatisticsKeys(LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String pattern = DAILY_STATS_KEY_PREFIX + "*:" + dateKey;
        
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null ? keys : Collections.emptySet();
    }

    @Override
    public Map<Object, Object> getDailyStatistics(String key) {
        Map<Object, Object> stats = redisTemplate.opsForHash().entries(key);
        return stats != null ? stats : Collections.emptyMap();
    }
}