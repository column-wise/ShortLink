package io.github.columnwise.shortlink.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.columnwise.shortlink.adapter.persistence.entity.UrlMetricsDailyEntity;
import io.github.columnwise.shortlink.adapter.persistence.entity.UrlMetricsHourlyEntity;
import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReaderPort;
import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyStatisticsChunkProcessor {

    private final UrlMetricsWriterPort urlMetricsWriterPort;
    private final RedisStatisticsReaderPort redisStatisticsReaderPort;
    private final ObjectMapper objectMapper;

    @Transactional
    public int processCodeChunk(List<String> codesToProcess, LocalDate targetDate) {
        int processedInChunk = 0;

        for (String code : codesToProcess) {
            try {
                long totalAccesses = urlMetricsWriterPort.sumHourlyAccesses(code, targetDate);
                long uniqueVisitors = redisStatisticsReaderPort.getDailyUniqueVisitorsCount(code, targetDate);

                Map<String, Long> devicePv = Optional.ofNullable(
                        redisStatisticsReaderPort.getDeviceStatistics(code, targetDate)
                ).orElseGet(Map::of);
                String devicePvJson = objectMapper.writeValueAsString(devicePv);

                UrlMetricsDailyEntity daily = urlMetricsWriterPort.findDailyMetrics(code, targetDate)
                        .orElseGet(() -> UrlMetricsDailyEntity.builder()
                                .code(code)
                                .day(targetDate)
                                .accesses(0L)
                                .uniqueVisitors(0L)
                                .deviceAccessesJson("{}")
                                .build());

                daily.setAccesses(totalAccesses);
                daily.setUniqueVisitors(uniqueVisitors);
                daily.setDeviceAccessesJson(devicePvJson);

                urlMetricsWriterPort.saveDailyMetrics(daily);
                processedInChunk++;

                log.debug("Aggregated daily statistics for code: {} on date: {}, PV: {}, UV: {}",
                        code, targetDate, totalAccesses, uniqueVisitors);

            } catch (Exception e) {
                log.error("Failed to aggregate daily statistics for code: {} on date: {}", code, targetDate, e);
            }
        }

        return processedInChunk;
    }
}

