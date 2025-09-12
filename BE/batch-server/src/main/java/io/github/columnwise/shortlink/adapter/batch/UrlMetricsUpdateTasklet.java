package io.github.columnwise.shortlink.adapter.batch;

import io.github.columnwise.shortlink.application.port.in.UpdateUrlMetricsUseCase;
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
public class UrlMetricsUpdateTasklet implements Tasklet {

    private final UpdateUrlMetricsUseCase updateUrlMetricsUseCase;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting URL metrics update tasklet");

        int updatedCount = updateUrlMetricsUseCase.updateTodayUrlMetrics();
        
        contribution.getStepExecution().getExecutionContext()
                   .put("updatedCount", updatedCount);

        log.info("URL metrics update tasklet completed. Updated {} URLs", updatedCount);

        return RepeatStatus.FINISHED;
    }
}