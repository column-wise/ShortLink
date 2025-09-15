package io.github.columnwise.shortlink.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * URL 단축 생성 응답 DTO
 *
 * <p>REST API를 통해 단축 URL 생성 성공 시 반환되는 데이터 전송 객체입니다.
 * 생성된 단축 코드와 완성된 단축 URL을 포함합니다.</p>
 *
 * <p>응답 내용:</p>
 * <ul>
 *   <li>code: 생성된 고유한 단축 코드</li>
 *   <li>shortUrl: 서버 URL과 결합된 완성된 단축 URL</li>
 * </ul>
 *
 * @param code 생성된 단축 코드
 * @param shortUrl 완성된 단축 URL (서버 도메인 + 경로 + 코드)
 */
@Schema(description = "URL 단축 응답")
public record CreateShortUrlResponse(
		@Schema(description = "생성된 단축 코드", example = "abc123")
		String code,
		
		@Schema(description = "완성된 단축 URL", example = "http://localhost:8080/api/v1/r/abc123")
		String shortUrl
) {}
