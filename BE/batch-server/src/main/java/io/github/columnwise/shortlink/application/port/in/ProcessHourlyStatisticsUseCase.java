package io.github.columnwise.shortlink.application.port.in;

import java.time.LocalDate;
import java.util.List;

/**
 * 시간별 통계 처리 유스케이스 (Inbound Port)
 *
 * Redis에서 날짜별 시간별 통계 데이터를 읽어와서 DB에 저장하는 배치 처리 로직입니다.
 * Redis 구조: url:hourly:access:{2025-09-15}:abc123 → Hash {"00":5, "01":12, ...}
 */
public interface ProcessHourlyStatisticsUseCase {

    /**
     * 직전 1시간의 모든 코드에 대해 시간별 통계를 처리합니다.
     * 현재 시간 - 1시간의 통계를 처리합니다.
     *
     * @return 처리된 코드 수
     */
    int processPreviousHour();
}