package io.github.columnwise.shortlink.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
		@NotBlank @Size(max = 2048) String longUrl
) {}