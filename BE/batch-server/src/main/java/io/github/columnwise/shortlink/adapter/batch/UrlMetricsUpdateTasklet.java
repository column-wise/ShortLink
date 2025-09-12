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

        // Job Parameters에서 targetDate 추출, 없으면 현재 날짜 사용
        String targetDateStr = chunkContext.getStepContext().getJobParameters().get("targetDate");
        java.time.LocalDate targetDate = targetDateStr != null ? 
            java.time.LocalDate.parse(targetDateStr.toString()) : java.time.LocalDate.now();
        
        log.info("Updating URL metrics for date: {}", targetDate);

        int updatedCount = updateUrlMetricsUseCase.updateUrlMetricsForDate(targetDate);
        
        contribution.getStepExecution().getExecutionContext()
                   .put("updatedCount", updatedCount)
                   .put("targetDate", targetDate.toString());

        log.info("URL metrics update tasklet completed. Updated {} URLs for date {}", 
                updatedCount, targetDate);

        return RepeatStatus.FINISHED;
    }
}