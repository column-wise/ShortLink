package io.github.columnwise.shortlink.application.port.in;

import java.time.LocalDate;

public interface AggregateStatisticsUseCase {
    
    /**
     * 지정된 날짜의 Redis 통계 데이터를 집계합니다.
     * 
     * @param targetDate 집계할 날짜
     * @return 처리된 URL 개수
     */
    int aggregateStatisticsForDate(LocalDate targetDate);
    
    /**
     * 오늘 날짜의 Redis 통계 데이터를 집계합니다.
     * 
     * @return 처리된 URL 개수
     */
    default int aggregateTodayStatistics() {
        return aggregateStatisticsForDate(LocalDate.now());
    }
}