package io.github.columnwise.shortlink.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@Schema(description = "URL 접속 로그")
public record UrlAccessLog(
        @Schema(description = "로그 ID", example = "1")
        long id,
        
        @Schema(description = "단축 코드", example = "abc123")
        String code,
        
        @Schema(description = "접속 IP 주소", example = "192.168.1.1")
        String ipAddress,
        
        @Schema(description = "사용자 에이전트", example = "Mozilla/5.0...")
        String userAgent,
        
        @Schema(description = "접속 시간", example = "2024-01-01T12:00:00Z")
        Instant accessedAt
) {
}
