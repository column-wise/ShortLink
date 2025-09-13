package io.github.columnwise.shortlink.adapter.redis;

import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStatisticsReaderAdapter implements RedisStatisticsReader {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ACCESS_COUNT_KEY_PREFIX = "url:access:count:";
    private static final String DAILY_STATS_KEY_PREFIX = "url:daily:stats:";

    @Override
    public Set<String> findAccessCountKeys(LocalDate date) {
        String pattern = ACCESS_COUNT_KEY_PREFIX + "*";
        String targetDatePrefix = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();
        
        Set<String> matchingKeys = new HashSet<>();
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (key.contains(targetDatePrefix)) {
                    matchingKeys.add(key);
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan access count keys for date: {}", date, e);
            return Collections.emptySet();
        }
        
        log.debug("Found {} access count keys for date: {}", matchingKeys.size(), date);
        return matchingKeys;
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
        
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();
        
        Set<String> matchingKeys = new HashSet<>();
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                matchingKeys.add(cursor.next());
            }
        } catch (Exception e) {
            log.error("Failed to scan daily statistics keys for date: {}", date, e);
            return Collections.emptySet();
        }
        
        return matchingKeys;
    }

    @Override
    public Map<Object, Object> getDailyStatistics(String key) {
        Map<Object, Object> stats = redisTemplate.opsForHash().entries(key);
        return stats != null ? stats : Collections.emptyMap();
    }
}