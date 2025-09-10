package io.github.columnwise.shortlink.domain.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ShortUrl(
		long id,
		String code,
		String longUrl,
		Instant createdAt,
		Instant expiresAt
) {
}
