package io.github.columnwise.shortlink.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * URL 단축 생성 요청 DTO
 *
 * <p>REST API를 통해 새로운 단축 URL 생성을 요청할 때 사용되는 데이터 전송 객체입니다.
 * Bean Validation을 통해 입력 데이터의 유효성을 검증합니다.</p>
 *
 * <p>유효성 및 제약 조건:</p>
 * <ul>
 *   <li>longUrl: 빈 값 불가, 최대 2048자</li>
 *   <li>HTTP/HTTPS 프로토콜을 포함한 완전한 URL 형식 권장</li>
 * </ul>
 *
 * @param longUrl 단축할 원본 URL (빈 값 불가, 최대 2048자)
 */
@Schema(description = "URL 단축 요청")
public record CreateShortUrlRequest(
		@Schema(
			description = "단축할 원본 URL", 
			example = "https://www.example.com/very/long/path/to/resource",
			requiredMode = Schema.RequiredMode.REQUIRED
		)
		@NotBlank @Size(max = 2048) String longUrl
) {}