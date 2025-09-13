package io.github.columnwise.shortlink.adapter.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.columnwise.shortlink.application.port.out.StatisticsRepository;
import io.github.columnwise.shortlink.domain.model.DailyStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CompositeStatisticsRepository implements StatisticsRepository {
    
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public List<DailyStatistics> getDailyStatistics(String code, LocalDate startDate, LocalDate endDate) {
        // 먼저 Redis 캐시에서 통계 조회 시도
        String cacheKey = "stats:" + code + ":" + startDate + ":" + endDate;
        List<DailyStatistics> cachedStats = getCachedStatistics(cacheKey);
        
        if (cachedStats != null && !cachedStats.isEmpty()) {
            return cachedStats;
        }
        
        // 캐시 미스: 실시간으로 Redis 방문 키들을 조회해서 계산
        List<DailyStatistics> result = calculateRealTimeStatistics(code, startDate, endDate);
        
        // 결과를 캐시에 저장 (5분 TTL)
        cacheStatistics(cacheKey, result, 5, java.util.concurrent.TimeUnit.MINUTES);
        
        return result;
    }
    
    private List<DailyStatistics> getCachedStatistics(String cacheKey) {
        try {
            Object cached = objectRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if (cached instanceof String jsonString) {
                    return objectMapper.readValue(jsonString, new TypeReference<List<DailyStatistics>>() {});
                } else if (cached instanceof List<?> list) {
                    return objectMapper.convertValue(list, new TypeReference<List<DailyStatistics>>() {});
                }
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve cached statistics for key: {}", cacheKey, e);
        }
        return null;
    }
    
    private List<DailyStatistics> calculateRealTimeStatistics(String code, LocalDate startDate, LocalDate endDate) {
        List<DailyStatistics> result = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            long accessCount = getAccessCountForDate(code, currentDate);
            
            // 0이 아닌 경우만 결과에 포함
            if (accessCount > 0) {
                result.add(DailyStatistics.builder()
                        .code(code)
                        .date(currentDate)
                        .accessCount(accessCount)
                        .uniqueVisitors(estimateUniqueVisitors(accessCount))
                        .build());
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return result;
    }
    
    private void cacheStatistics(String cacheKey, List<DailyStatistics> statistics, long timeout, java.util.concurrent.TimeUnit unit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(statistics);
            objectRedisTemplate.opsForValue().set(cacheKey, jsonValue, timeout, unit);
            log.debug("Cached statistics for key: {} with {} entries", cacheKey, statistics.size());
        } catch (Exception e) {
            log.warn("Failed to cache statistics for key: {}", cacheKey, e);
        }
    }
    
    @Override
    public long getAccessCountForDate(String code, LocalDate date) {
        String pattern = "url:access:count:" + code + ":*";
        String targetDatePrefix = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();
        
        long count = 0;
        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (key.contains(targetDatePrefix)) {
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan Redis keys for pattern: {} on date: {}", pattern, date, e);
            return 0;
        }
        
        return count;
    }
    
    private long estimateUniqueVisitors(long accessCount) {
        if (accessCount <= 0) {
            return 0;
        }
        
        // 현실적인 고유 방문자 추정 알고리즘:
        // - 낮은 접근수: 고유 방문자 비율 높음 (90-95%)
        // - 중간 접근수: 중간 비율 (70-80%)  
        // - 높은 접근수: 중복 증가로 비율 감소 (50-70%)
        
        double uniqueRatio;
        if (accessCount <= 10) {
            // 1-10 접근: 거의 모두 고유 방문자
            uniqueRatio = 0.95 - (accessCount - 1) * 0.02; // 95% -> 77%
        } else if (accessCount <= 100) {
            // 11-100 접근: 점진적 중복 증가
            uniqueRatio = 0.77 - Math.log10(accessCount - 9) * 0.15; // 77% -> 62%
        } else {
            // 100+ 접근: 상당한 중복, 로그 스케일로 감소
            uniqueRatio = 0.62 - Math.log10(accessCount / 100.0) * 0.1;
            uniqueRatio = Math.max(uniqueRatio, 0.3); // 최소 30% 보장
        }
        
        return Math.max(1, Math.round(accessCount * uniqueRatio));
    }
}