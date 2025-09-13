package io.github.columnwise.shortlink.adapter.batch;

import io.github.columnwise.shortlink.application.port.in.AggregateStatisticsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsAggregationTasklet implements Tasklet {

    private final AggregateStatisticsUseCase aggregateStatisticsUseCase;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting statistics aggregation tasklet");

        // Job Parameters에서 targetDate 추출, 없으면 UTC 기준 현재 날짜 사용
        Object targetDateObj = chunkContext.getStepContext().getJobParameters().get("targetDate");
        LocalDate targetDate = parseTargetDate(targetDateObj);
        
        log.info("Processing statistics for date: {}", targetDate);

        int processedCount = aggregateStatisticsUseCase.aggregateStatisticsForDate(targetDate);
        
        contribution.getStepExecution().getExecutionContext()
                   .put("processedCount", processedCount);
        contribution.getStepExecution().getExecutionContext()
                   .put("targetDate", targetDate.toString());

        log.info("Statistics aggregation tasklet completed. Processed {} items for date {}", 
                processedCount, targetDate);

        return RepeatStatus.FINISHED;
    }
    
    /**
     * 다양한 형식의 날짜 입력을 파싱하여 LocalDate로 변환
     * 
     * @param targetDateObj Job Parameter로 전달된 날짜 객체
     * @return 파싱된 LocalDate, 실패 시 UTC 기준 현재 날짜
     */
    private LocalDate parseTargetDate(Object targetDateObj) {
        if (targetDateObj == null) {
            return LocalDate.now(ZoneId.of("UTC"));
        }
        
        String dateString = targetDateObj.toString().trim();
        if (dateString.isEmpty()) {
            return LocalDate.now(ZoneId.of("UTC"));
        }
        
        // 다양한 날짜 형식 시도
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ISO_LOCAL_DATE,      // 2024-09-13
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),    // 2024/09/13
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),    // 2024-09-13
            DateTimeFormatter.ofPattern("yyyyMMdd")       // 20240913
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate parsed = LocalDate.parse(dateString, formatter);
                log.debug("Successfully parsed target date: {} -> {}", dateString, parsed);
                return parsed;
            } catch (DateTimeParseException e) {
                // 다음 포맷터 시도
            }
        }
        
        log.warn("Failed to parse target date: '{}', using UTC current date", dateString);
        return LocalDate.now(ZoneId.of("UTC"));
    }
}