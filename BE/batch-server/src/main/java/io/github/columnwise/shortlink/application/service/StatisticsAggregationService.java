package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.AggregateStatisticsUseCase;
import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReader;
import io.github.columnwise.shortlink.application.port.out.StatisticsWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public int aggregateStatisticsForDate(LocalDate targetDate) {
        log.info("Starting statistics aggregation for date: {}", targetDate);

        Set<String> accessKeys = statisticsReader.findAccessCountKeys(targetDate);
        
        if (accessKeys.isEmpty()) {
            log.info("No access statistics found for date: {}", targetDate);
            return 0;
        }

        int processedCount = 0;
        for (String accessKey : accessKeys) {
            try {
                String code = extractCodeFromAccessKey(accessKey, targetDate);
                
                Long accessCount = statisticsReader.getAccessCount(accessKey);
                if (accessCount == null) {
                    continue;
                }
                
                // 일일 통계 저장
                statisticsWriter.saveDailyStatistics(code, targetDate, accessCount, 1, TimeUnit.DAYS);
                
                // 처리 완료 마킹
                statisticsWriter.markAsProcessed(code, targetDate, 1, TimeUnit.DAYS);
                
                processedCount++;
                
            } catch (Exception e) {
                log.error("Error processing access key: {}", accessKey, e);
            }
        }

        log.info("Completed statistics aggregation for date: {}. Processed {} keys", 
                targetDate, processedCount);

        return processedCount;
    }

    private String extractCodeFromAccessKey(String accessKey, LocalDate date) {
        // url:access:count:ABC123:2024-01-01에서 ABC123 추출
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String prefix = "url:access:count:";
        String suffix = ":" + dateKey;
        
        int startIndex = prefix.length();
        int endIndex = accessKey.length() - suffix.length();
        
        return accessKey.substring(startIndex, endIndex);
    }
}