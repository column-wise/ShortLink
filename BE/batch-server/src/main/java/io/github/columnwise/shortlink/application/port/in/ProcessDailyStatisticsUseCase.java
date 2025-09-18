package io.github.columnwise.shortlink.application.port.in;

import java.time.LocalDate;

/**
 * 일별 통계 처리 유스케이스 (Inbound Port)
 *
 * DB의 시간별 데이터를 읽어와서 일별로 집계하여 저장하는 배치 처리 로직입니다.
 * url_metrics_hourly → url_metrics_daily 집계
 */
public interface ProcessDailyStatisticsUseCase {

    /**
     * 특정 날짜의 모든 코드에 대해 시간별 → 일별 집계를 수행합니다.
     *
     * 처리 과정:
     * 1. url_metrics_hourly에서 해당 날짜의 24시간 데이터 조회
     * 2. 시간별 accesses 합산 → 일별 total accesses
     * 3. 시간별 HLL 병합 → 일별 unique visitors
     * 4. 디바이스별 데이터 집계
     * 5. url_metrics_daily에 저장/업데이트
     *
     * @param targetDate 집계할 날짜
     * @return 집계된 코드 수
     */
    int processDailyStatistics(LocalDate targetDate);

    /**
     * 특정 코드의 특정 날짜에 대해 시간별 → 일별 집계를 수행합니다.
     *
     * @param code 단축 코드
     * @param targetDate 집계할 날짜
     * @return 집계 성공 여부
     */
    boolean processCodeDailyStatistics(String code, LocalDate targetDate);
}