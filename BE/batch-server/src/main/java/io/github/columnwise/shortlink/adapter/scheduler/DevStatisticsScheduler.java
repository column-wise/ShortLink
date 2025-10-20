package io.github.columnwise.shortlink.adapter.scheduler;

import io.github.columnwise.shortlink.application.port.in.ProcessHourlyStatisticsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dev-only scheduler to make verification easier.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevStatisticsScheduler {

    private final ProcessHourlyStatisticsUseCase hourlyUseCase;

    // Run every 1 minute in dev to aggregate previous hour
    @Scheduled(fixedDelayString = "60000", initialDelayString = "15000")
    public void aggregateHourlyDev() {
        try {
            int processed = hourlyUseCase.processCurrentHour();
            if (processed > 0) {
                log.info("[DevHourlyAggregation] processed={} items", processed);
            }
        } catch (Exception e) {
            log.error("[DevHourlyAggregation] failed", e);
        }
    }
}

