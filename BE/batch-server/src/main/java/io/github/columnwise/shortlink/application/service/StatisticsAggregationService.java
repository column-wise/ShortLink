package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.AggregateStatisticsUseCase;
import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReader;
import io.github.columnwise.shortlink.application.port.out.StatisticsWriter;
import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsAggregationService implements AggregateStatisticsUseCase {

    private final RedisStatisticsReader statisticsReader;
    private final StatisticsWriter statisticsWriter;
    private final UrlMetricsWriter urlMetricsWriter;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public int aggregateStatisticsForDate(LocalDate targetDate) {
        log.info("Starting statistics aggregation for date: {}", targetDate);

        Set<String> accessKeys = statisticsReader.findAccessCountKeys(targetDate);
        
        if (accessKeys.isEmpty()) {
            log.info("No access statistics found for date: {}", targetDate);
            return 0;
        }

        // 코드별로 그룹핑해서 카운트
        java.util.Map<String, Long> codeCountMap = new java.util.HashMap<>();
        for (String accessKey : accessKeys) {
            try {
                String code = extractCodeFromAccessKey(accessKey, targetDate);
                codeCountMap.put(code, codeCountMap.getOrDefault(code, 0L) + 1);
            } catch (Exception e) {
                log.error("Error processing access key: {}", accessKey, e);
            }
        }
        
        // 각 코드별로 통계 저장
        int processedCount = 0;
        for (java.util.Map.Entry<String, Long> entry : codeCountMap.entrySet()) {
            try {
                String code = entry.getKey();
                Long accessCount = entry.getValue();
                
                // 일일 통계 저장
                statisticsWriter.saveDailyStatistics(code, targetDate, accessCount, 1, TimeUnit.DAYS);
                
                // URL 전체 통계 DB에 저장 (총 접근 횟수 증가)
                urlMetricsWriter.incrementTotalAccessCount(code, accessCount);
                
                // 처리 완료 마킹
                statisticsWriter.markAsProcessed(code, targetDate, 1, TimeUnit.DAYS);
                
                // 통계 캐시 무효화 (새로운 데이터가 처리되었으므로)
                invalidateStatisticsCache(code);
                
                processedCount++;
                
            } catch (Exception e) {
                log.error("Error processing code statistics: {}", entry.getKey(), e);
            }
        }

        log.info("Completed statistics aggregation for date: {}. Processed {} keys", 
                targetDate, processedCount);

        return processedCount;
    }
    
    private void invalidateStatisticsCache(String code) {
        try {
            // 해당 코드의 모든 통계 캐시 키를 삭제
            String pattern = "stats:" + code + ":*";
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated statistics cache for code: {}, keys: {}", code, keys.size());
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate statistics cache for code: {}", code, e);
        }
    }

    private String extractCodeFromAccessKey(String accessKey, LocalDate date) {
        // url:access:count:ABC123:2024-01-01T14:30:45에서 ABC123 추출
        String prefix = "url:access:count:";
        
        // prefix 이후 부분을 가져온다
        String remaining = accessKey.substring(prefix.length());
        
        // 첫 번째 콜론까지가 코드
        int colonIndex = remaining.indexOf(':');
        if (colonIndex == -1) {
            // 콜론이 없으면 전체가 코드 (타임스탬프가 없는 경우)
            return remaining;
        }
        
        return remaining.substring(0, colonIndex);
    }
}