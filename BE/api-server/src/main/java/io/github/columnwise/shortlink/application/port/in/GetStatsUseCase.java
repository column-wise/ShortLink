package io.github.columnwise.shortlink.application.port.in;

import io.github.columnwise.shortlink.domain.model.DailyStatistics;
import java.time.LocalDate;
import java.util.List;

public interface GetStatsUseCase {
    
    /**
     * 특정 기간의 일별 통계를 조회합니다.
     * 
     * @param code 단축 코드
     * @param startDate 시작 날짜 (null이면 30일 전)
     * @param endDate 종료 날짜 (null이면 오늘)
     * @return 일별 통계 목록
     */
    List<DailyStatistics> getDailyStatistics(String code, LocalDate startDate, LocalDate endDate);
}
