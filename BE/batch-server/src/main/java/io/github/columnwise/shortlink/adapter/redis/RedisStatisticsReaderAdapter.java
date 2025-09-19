package io.github.columnwise.shortlink.adapter.redis;

import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReaderPort;
import io.github.columnwise.shortlink.domain.service.RedisKeyManager;
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

/**
 * Redis 통계 데이터 읽기 어댑터 구현체
 *
 * API 서버가 저장한 Redis 통계 데이터를 읽어오는 어댑터입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStatisticsReaderAdapter implements RedisStatisticsReaderPort {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Set<String> findCodesWithStatistics(LocalDate date) {
        String pattern = RedisKeyManager.getHourlyAccessKey("*", date);

        Set<String> codes = new HashSet<>();
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String code = extractCodeFromKey(key);
                if (code != null) {
                    codes.add(code);
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan codes for date: {}", date, e);
        }

        log.debug("Found {} codes with statistics for date: {}", codes.size(), date);
        return codes;
    }

    @Override
    public Map<String, Long> getHourlyAccesses(String code, LocalDate date) {
        String key = RedisKeyManager.getHourlyAccessKey(code, date);

        try {
            Map<Object, Object> hourlyData = redisTemplate.opsForHash().entries(key);
            if (hourlyData.isEmpty()) {
                return Collections.emptyMap();
            }

            return hourlyData.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().toString(),
                            entry -> Long.parseLong(entry.getValue().toString())
                    ));
        } catch (Exception e) {
            log.error("Failed to get hourly accesses for code: {}, date: {}", code, date, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Set<String> findCodesWithStatistics(LocalDate date, int hour) {
        // 시간별 인덱스가 있다면 사용, 없으면 전체 스캔 후 필터링
        return findCodesWithStatistics(date);
    }

    @Override
    public long getDailyUniqueVisitorsCount(String code, LocalDate date) {
        String key = RedisKeyManager.getDailyUniqueKey(code, date);

        try {
            // HLL PFCOUNT로 유니크 방문자 수 계산
            Long count = redisTemplate.opsForHyperLogLog().size(key);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Failed to get unique visitors count for code: {}, date: {}", code, date, e);
            return 0L;
        }
    }

    @Override
    public Map<String, Long> getUserAgentStatistics(String code, LocalDate date) {
        String key = RedisKeyManager.getDailyUaKey(code, date);
        return getHashStatistics(key, code, date, "user agent");
    }

    @Override
    public Map<String, Long> getDeviceStatistics(String code, LocalDate date) {
        String key = RedisKeyManager.getDailyDeviceKey(code, date);
        return getHashStatistics(key, code, date, "device");
    }


    @Override
    public int cleanupProcessedData(String code, LocalDate date) {
        List<String> keysToDelete = Arrays.asList(
                RedisKeyManager.getHourlyAccessKey(code, date),
                RedisKeyManager.getDailyUniqueKey(code, date),
                RedisKeyManager.getDailyUaKey(code, date),
                RedisKeyManager.getDailyDeviceKey(code, date)
        );

        try {
            Long deletedCount = redisTemplate.delete(keysToDelete);
            log.debug("Cleaned up {} keys for code: {}, date: {}", deletedCount, code, date);
            return deletedCount != null ? deletedCount.intValue() : 0;
        } catch (Exception e) {
            log.error("Failed to cleanup data for code: {}, date: {}", code, date, e);
            return 0;
        }
    }

    private Map<String, Long> getHashStatistics(String key, String code, LocalDate date, String type) {
        try {
            Map<Object, Object> hashData = redisTemplate.opsForHash().entries(key);
            if (hashData.isEmpty()) {
                return Collections.emptyMap();
            }

            return hashData.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().toString(),
                            entry -> Long.parseLong(entry.getValue().toString())
                    ));
        } catch (Exception e) {
            log.error("Failed to get {} statistics for code: {}, date: {}", type, code, date, e);
            return Collections.emptyMap();
        }
    }

    private String extractCodeFromKey(String key) {
        // url:hourly:access:{2025-09-15}:abc123 → abc123
        if (key == null || key.isEmpty()) {
            return null;
        }

        int lastColonIndex = key.lastIndexOf(':');
        if (lastColonIndex > 0 && lastColonIndex < key.length() - 1) {
            return key.substring(lastColonIndex + 1);
        }

        return null;
    }
}