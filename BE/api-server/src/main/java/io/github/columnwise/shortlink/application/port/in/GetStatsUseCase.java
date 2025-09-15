package io.github.columnwise.shortlink.application.port.in;

import io.github.columnwise.shortlink.domain.model.UrlMetrics;

public interface GetStatsUseCase {

    /**
     * 특정 단축 URL의 누적 통계를 조회합니다.
     *
     * @param code 단축 코드
     * @return URL 누적 통계 (총 조회수, 오늘 조회수 등)
     */
    UrlMetrics getUrlMetrics(String code);
}
