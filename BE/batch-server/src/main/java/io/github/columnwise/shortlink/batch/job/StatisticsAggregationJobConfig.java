package io.github.columnwise.shortlink.batch.job;

import io.github.columnwise.shortlink.adapter.batch.StatisticsAggregationTasklet;
import io.github.columnwise.shortlink.adapter.batch.UrlMetricsUpdateTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StatisticsAggregationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final StatisticsAggregationTasklet statisticsAggregationTasklet;
    private final UrlMetricsUpdateTasklet urlMetricsUpdateTasklet;

    @Bean
    public Job statisticsAggregationJob() {
        return new JobBuilder("statisticsAggregationJob", jobRepository)
                .start(aggregateRedisStatisticsStep())
                .next(updateUrlStatisticsStep())
                .build();
    }

    @Bean
    public Step aggregateRedisStatisticsStep() {
        return new StepBuilder("aggregateRedisStatisticsStep", jobRepository)
                .tasklet(statisticsAggregationTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step updateUrlStatisticsStep() {
        return new StepBuilder("updateUrlStatisticsStep", jobRepository)
                .tasklet(urlMetricsUpdateTasklet, transactionManager)
                .build();
    }
}