package io.github.columnwise.shortlink.domain.model;

import lombok.Builder;

/**
 * URL 메트릭 도메인 모델
 *
 * <p>단축 URL에 대한 누적 통계 정보를 담는 객체입니다.
 * <b>핫패스(리다이렉트 요청)</b>에서는 Redis에만 기록을 남기고,
 * <b>배치 서버</b>가 주기적으로 Redis의 집계 결과를 읽어
 * 데이터베이스에 영속화할 때 사용됩니다.</p>
 *
 * <p>주요 속성:</p>
 * <ul>
 *   <li>code: 단축 코드 (ShortUrl과 1:1 대응)</li>
 *   <li>totalAccesses: 지금까지의 누적 방문 수 (어제까지 집계 + 오늘 Redis 값)</li>
 * </ul>
 *
 * <p>이 모델은 단축 URL 자체의 메타데이터(ShortUrl)와
 * 집계 데이터(UrlMetrics)를 분리하여 관리하기 위해 존재합니다.
 * 대규모 트래픽 환경에서도 충돌 없이 확장할 수 있도록 설계되었습니다.</p>
 *
 * @param code 단축 코드
 * @param totalAccesses 누적 방문 수
 */
@Builder
public record UrlMetrics(
		String code,
		long totalAccesses
) {
}
