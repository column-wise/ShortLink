package io.github.columnwise.shortlink.adapter.scheduler;

import io.github.columnwise.shortlink.application.port.in.ProcessDailyStatisticsUseCase;
import io.github.columnwise.shortlink.application.port.in.ProcessHourlyStatisticsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedStatisticsJobScheduler {

    private final ProcessHourlyStatisticsUseCase hourlyUseCase;
    private final ProcessDailyStatisticsUseCase dailyUseCase;

    // 매 시 5분에 이전 시각(직전 1시간) 집계
    @Scheduled(cron = "0 5 * * * *")
    public void aggregateHourly() {
        try {
            int processed = hourlyUseCase.processCurrentHour();
            log.info("[HourlyAggregation] processed={} items", processed);
        } catch (Exception e) {
            log.error("[HourlyAggregation] failed", e);
        }
    }

    // 매일 00:10에 전일 일자 집계
    @Scheduled(cron = "0 10 0 * * *")
    public void aggregateDaily() {
        LocalDate target = LocalDate.now().minusDays(1);
        try {
            int processed = dailyUseCase.processDailyStatistics(target);
            log.info("[DailyAggregation] date={} processed={} items", target, processed);
        } catch (Exception e) {
            log.error("[DailyAggregation] date={} failed", target, e);
        }
    }
}
