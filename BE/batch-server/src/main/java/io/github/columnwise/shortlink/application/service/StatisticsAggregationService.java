package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.AggregateStatisticsUseCase;
import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReader;
import io.github.columnwise.shortlink.application.port.out.StatisticsWriter;
import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriter;
import io.github.columnwise.shortlink.domain.service.RedisKeyManager;
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
                
                // 중복 처리 방지: 이미 처리된 코드/날짜 조합인지 확인
                if (isAlreadyProcessed(code, targetDate)) {
                    log.debug("Statistics for code {} and date {} already processed, skipping", code, targetDate);
                    continue;
                }
                
                // 처리 중 마킹 (다른 인스턴스에서 동시 처리 방지)
                if (!markAsProcessing(code, targetDate)) {
                    log.debug("Code {} for date {} is being processed by another instance, skipping", code, targetDate);
                    continue;
                }
                
                try {
                    // 일일 통계 저장
                    statisticsWriter.saveDailyStatistics(code, targetDate, accessCount, 1, TimeUnit.DAYS);
                    
                    // URL 전체 통계 DB에 저장 (총 접근 횟수 증가)
                    urlMetricsWriter.incrementTotalAccessCount(code, accessCount);
                    
                    // 처리 완료 마킹
                    statisticsWriter.markAsProcessed(code, targetDate, 1, TimeUnit.DAYS);
                    
                    // 통계 캐시 무효화 (새로운 데이터가 처리되었으므로)
                    invalidateStatisticsCache(code);
                    
                    processedCount++;
                    
                } finally {
                    // 처리 중 상태 해제
                    clearProcessingMark(code, targetDate);
                }
                
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

    private boolean isAlreadyProcessed(String code, LocalDate date) {
        try {
            String processedKey = RedisKeyManager.getProcessedMarkerKey(code, date);
            return Boolean.TRUE.equals(redisTemplate.hasKey(processedKey));
        } catch (Exception e) {
            log.warn("Failed to check processed status for code: {}, date: {}", code, date, e);
            return false;
        }
    }
    
    private boolean markAsProcessing(String code, LocalDate date) {
        try {
            String processingKey = RedisKeyManager.getProcessingMarkerKey(code, date);
            // 10분 TTL로 처리 중 마킹 (처리가 오래 걸리거나 실패해도 자동 해제)
            Boolean result = redisTemplate.opsForValue().setIfAbsent(processingKey, "processing", 10, TimeUnit.MINUTES);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Failed to mark as processing for code: {}, date: {}", code, date, e);
            return false;
        }
    }
    
    private void clearProcessingMark(String code, LocalDate date) {
        try {
            String processingKey = RedisKeyManager.getProcessingMarkerKey(code, date);
            redisTemplate.delete(processingKey);
        } catch (Exception e) {
            log.warn("Failed to clear processing mark for code: {}, date: {}", code, date, e);
        }
    }

    private String extractCodeFromAccessKey(String accessKey, LocalDate date) {
        // RedisKeyManager를 사용하여 일관된 키 파싱
        return RedisKeyManager.extractCodeFromKey(accessKey);
    }
}