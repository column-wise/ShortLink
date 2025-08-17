package io.github.columnwise.shortlink.domain.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UrlAccessLog(
        long id,
        String code,
        String ipAddress,
        String userAgent,
        Instant accessedAt
) {
}
