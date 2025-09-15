package io.github.columnwise.shortlink.domain.model;

import lombok.Builder;

import java.time.Instant;

/**
 * 단축 URL 도메인 모델
 *
 * <p>URL 단축 서비스의 핵심 도메인 객체로, 단축된 URL의 모든 정보를 담고 있습니다.
 * 기본적으로 불변 객체(record)로 설계되어 스레드 안전성을 보장합니다.</p>
 *
 * <p>주요 속성:</p>
 * <ul>
 *   <li>id: 데이터베이스 기본 키 (Long 타입)</li>
 *   <li>code: 고유한 단축 코드 (사용자에게 노출되는 식별자)</li>
 *   <li>longUrl: 원본 URL (리다이렉트 대상)</li>
 *   <li>createdAt: 생성 시간</li>
 *   <li>expiresAt: 만료 시간</li>
 * </ul>
 *
 * @param id 데이터베이스 기본 키
 * @param code 고유한 단축 코드
 * @param longUrl 원본 URL
 * @param createdAt 생성 시간
 * @param expiresAt 만료 시간
 */
@Builder
public record ShortUrl(
		long id,
		String code,
		String longUrl,
		Instant createdAt,
		Instant expiresAt
) {
}
