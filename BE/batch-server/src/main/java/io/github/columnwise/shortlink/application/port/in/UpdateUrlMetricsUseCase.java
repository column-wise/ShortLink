package io.github.columnwise.shortlink.application.port.in;

import java.time.LocalDate;

public interface UpdateUrlMetricsUseCase {
    
    /**
     * 지정된 날짜의 URL 메트릭을 업데이트합니다.
     * 
     * @param targetDate 업데이트할 날짜
     * @return 업데이트된 URL 개수
     */
    int updateUrlMetricsForDate(LocalDate targetDate);
    
    /**
     * 오늘 날짜의 URL 메트릭을 업데이트합니다.
     * 
     * @return 업데이트된 URL 개수
     */
    default int updateTodayUrlMetrics() {
        return updateUrlMetricsForDate(LocalDate.now());
    }
}