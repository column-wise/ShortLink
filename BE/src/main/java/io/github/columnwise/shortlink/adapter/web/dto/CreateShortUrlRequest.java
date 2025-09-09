package io.github.columnwise.shortlink.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "URL 단축 요청")
public record CreateShortUrlRequest(
		@Schema(
			description = "단축할 원본 URL", 
			example = "https://www.example.com/very/long/path/to/resource",
			requiredMode = Schema.RequiredMode.REQUIRED
		)
		@NotBlank @Size(max = 2048) String longUrl
) {}