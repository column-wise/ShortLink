package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.ProcessDailyStatisticsUseCase;
import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriterPort;
import io.github.columnwise.shortlink.adapter.persistence.entity.UrlMetricsHourlyEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessDailyStatisticsService implements ProcessDailyStatisticsUseCase {

    private final UrlMetricsWriterPort urlMetricsWriterPort;
    private final DailyStatisticsChunkProcessor chunkProcessor;

    private static final int DEFAULT_CHUNK_SIZE = 1000;

    @Override
    public int processDailyStatistics(LocalDate targetDate) {
        return processDailyStatistics(targetDate, DEFAULT_CHUNK_SIZE);
    }

    public int processDailyStatistics(LocalDate targetDate, int chunkSize) {
        log.info("Processing hourly data to daily aggregation for date: {} with chunk size: {}", targetDate, chunkSize);

        List<UrlMetricsHourlyEntity> hourlyMetrics = urlMetricsWriterPort.findAllHourlyMetricsByDate(targetDate);
        if (hourlyMetrics.isEmpty()) {
            log.info("No hourly metrics found for date: {}", targetDate);
            return 0;
        }

        log.info("Found {} hourly metrics records for date: {}", hourlyMetrics.size(), targetDate);

        List<String> codes = hourlyMetrics.stream()
                .map(UrlMetricsHourlyEntity::getCode)
                .distinct()
                .collect(Collectors.toList());

        int totalCodes = codes.size();
        int processedCodes = 0;
        int failedCodes = 0;

        log.info("Processing {} codes in chunks of {}", totalCodes, chunkSize);

        for (int i = 0; i < totalCodes; i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, totalCodes);
            List<String> chunk = codes.subList(i, endIndex);

            log.info("Processing chunk {}-{} of {} codes", i + 1, endIndex, totalCodes);

            try {
                int chunkProcessed = chunkProcessor.processCodeChunk(chunk, targetDate);
                processedCodes += chunkProcessed;
                failedCodes += (chunk.size() - chunkProcessed);
            } catch (Exception e) {
                log.error("Failed to process chunk {}-{} on date: {}", i + 1, endIndex, targetDate, e);
                failedCodes += chunk.size();
            }
        }

        log.info("Successfully processed {}/{} codes for daily aggregation on date: {} (failed: {})",
                processedCodes, totalCodes, targetDate, failedCodes);
        return processedCodes;
    }

    @Override
    public boolean processCodeDailyStatistics(String code, LocalDate targetDate) {
        try {
            return chunkProcessor.processCodeChunk(List.of(code), targetDate) > 0;
        } catch (Exception e) {
            log.error("Failed to process single code daily statistics: {} on date: {}", code, targetDate, e);
            return false;
        }
    }
}

