package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.GetStatsUseCase;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.model.UrlMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 통계 조회 서비스 구현체
 *
 * <p>단축 URL의 누적 통계를 조회하는 서비스입니다.
 * OLAP DB에 접근하여 통계 정보를 조회합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>누적 통계 조회 (총 접속수)</li>
 *   <li>OLAP DB 기록 데이터</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GetStatsService implements GetStatsUseCase {

    private final ShortUrlRepositoryPort shortUrlRepositoryPort;

	/**
	 * 특정 단축 URL의 누적 통계를 조회합니다.
	 *
	 * @param code 단축 코드
	 * @return URL 누적 통계 정보
	 * @throws UrlNotFoundException 단축 코드가 존재하지 않는 경우
	 */
	@Override
	public UrlMetrics getUrlMetrics(String code) {
		// 먼저 코드가 존재하는지 확인
		shortUrlRepositoryPort.findByCode(code)
				.orElseThrow(() -> new UrlNotFoundException("URL not found for code: " + code));

        return UrlMetrics.builder()
                .code(code)
                // TODO(olap): OLAP 집계 연동 전까지 0 반환
                .totalAccesses(0)
                .build();
    }
}
