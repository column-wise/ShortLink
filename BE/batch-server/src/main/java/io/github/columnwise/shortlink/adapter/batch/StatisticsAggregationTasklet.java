package io.github.columnwise.shortlink.adapter.batch;

import io.github.columnwise.shortlink.application.port.in.AggregateStatisticsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsAggregationTasklet implements Tasklet {

    private final AggregateStatisticsUseCase aggregateStatisticsUseCase;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting statistics aggregation tasklet");

        int processedCount = aggregateStatisticsUseCase.aggregateTodayStatistics();
        
        contribution.getStepExecution().getExecutionContext()
                   .put("processedCount", processedCount);

        log.info("Statistics aggregation tasklet completed. Processed {} items", processedCount);

        return RepeatStatus.FINISHED;
    }
}