package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.GetStatsUseCase;
import io.github.columnwise.shortlink.application.port.out.StatisticsRepository;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.model.DailyStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 통계 조회 서비스 구현체
 *
 * <p>단축 URL의 일별 접속 통계를 조회하는 서비스입니다.
 * 지정된 기간 내의 통계 데이터를 제공하며, 기본값 처리를 통해 사용성을 향상시켰습니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>일별 접속 통계 조회</li>
 *   <li>날짜 범위 기본값 설정 (30일 전~오늘)</li>
 *   <li>유연한 기간 설정 지원</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GetStatsService implements GetStatsUseCase {

    private final StatisticsRepository statisticsRepository;
    private final ShortUrlRepositoryPort shortUrlRepositoryPort;

    /**
     * 지정된 기간 내의 일별 접속 통계를 조회합니다.
     *
     * <p>먼저 단축 코드가 존재하는지 검증한 후, 통계 데이터를 조회합니다.
     * 시작일과 종료일이 null인 경우 기본값을 설정합니다:
     * <ul>
     *   <li>endDate가 null이면 오늘 날짜로 설정</li>
     *   <li>startDate가 null이면 endDate에서 30일 전으로 설정</li>
     * </ul></p>
     *
     * @param code 단축 코드
     * @param startDate 시작 날짜 (null 가능, 기본값: 30일 전)
     * @param endDate 종료 날짜 (null 가능, 기본값: 오늘)
     * @return 일별 통계 목록
     * @throws UrlNotFoundException 단축 코드가 존재하지 않는 경우
     */
    @Override
    public List<DailyStatistics> getDailyStatistics(String code, LocalDate startDate, LocalDate endDate) {
        // 먼저 코드가 존재하는지 확인
        shortUrlRepositoryPort.findByCode(code)
            .orElseThrow(() -> new UrlNotFoundException("URL not found for code: " + code));

        // 기본값 설정
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);  // 기본 30일
        }

        return statisticsRepository.getDailyStatistics(code, startDate, endDate);
    }
}
