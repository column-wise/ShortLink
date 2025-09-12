package io.github.columnwise.shortlink.adapter.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job statisticsAggregationJob;

    /**
     * 매시간 정각에 통계 집계 배치 실행
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void runStatisticsAggregation() {
        try {
            log.info("Starting scheduled statistics aggregation job");
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            
            jobLauncher.run(statisticsAggregationJob, jobParameters);
            
            log.info("Statistics aggregation job completed successfully");
            
        } catch (Exception e) {
            log.error("Failed to run statistics aggregation job", e);
        }
    }

    /**
     * 매일 자정에 일일 통계 집계 배치 실행 (추가 처리용)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void runDailyStatisticsAggregation() {
        try {
            log.info("Starting daily statistics aggregation job");
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("type", "daily")
                    .toJobParameters();
            
            jobLauncher.run(statisticsAggregationJob, jobParameters);
            
            log.info("Daily statistics aggregation job completed successfully");
            
        } catch (Exception e) {
            log.error("Failed to run daily statistics aggregation job", e);
        }
    }
}