package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.UpdateUrlMetricsUseCase;
import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReader;
import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlMetricsUpdateService implements UpdateUrlMetricsUseCase {

    private final RedisStatisticsReader statisticsReader;
    private final UrlMetricsWriter metricsWriter;

    @Override
    public int updateUrlMetricsForDate(LocalDate targetDate) {
        log.info("Starting URL metrics update for date: {}", targetDate);

        Set<String> dailyStatsKeys = statisticsReader.findDailyStatisticsKeys(targetDate);
        
        if (dailyStatsKeys.isEmpty()) {
            log.info("No daily statistics found for date: {}", targetDate);
            return 0;
        }

        int updatedCount = 0;
        for (String dailyStatsKey : dailyStatsKeys) {
            try {
                String code = extractCodeFromDailyStatsKey(dailyStatsKey, targetDate);
                
                Map<Object, Object> dailyStats = statisticsReader.getDailyStatistics(dailyStatsKey);
                if (dailyStats.isEmpty()) {
                    continue;
                }
                
                Object accessCountObj = dailyStats.get("accessCount");
                if (accessCountObj == null) {
                    continue;
                }
                
                long dailyAccessCount = Long.parseLong(accessCountObj.toString());
                
                // 총 접근 횟수 업데이트
                metricsWriter.incrementTotalAccessCount(code, dailyAccessCount);
                
                // 마지막 접근 시간 업데이트 (현재 시간으로)
                metricsWriter.updateLastAccessTime(code, System.currentTimeMillis());
                
                log.debug("Updated metrics for code {}: daily={}", code, dailyAccessCount);
                
                updatedCount++;
                
            } catch (Exception e) {
                log.error("Error updating metrics for key: {}", dailyStatsKey, e);
            }
        }

        log.info("Completed URL metrics update for date: {}. Updated {} URLs", 
                targetDate, updatedCount);

        return updatedCount;
    }

    private String extractCodeFromDailyStatsKey(String dailyStatsKey, LocalDate date) {
        // url:daily:stats:ABC123:2024-01-01에서 ABC123 추출
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String prefix = "url:daily:stats:";
        String suffix = ":" + dateKey;
        
        int startIndex = prefix.length();
        int endIndex = dailyStatsKey.length() - suffix.length();
        
        return dailyStatsKey.substring(startIndex, endIndex);
    }
}