package io.github.columnwise.shortlink.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "URL 단축 응답")
public record CreateShortUrlResponse(
		@Schema(description = "생성된 단축 코드", example = "abc123")
		String code,
		
		@Schema(description = "완성된 단축 URL", example = "http://localhost:8080/api/v1/r/abc123")
		String shortUrl
) {}
