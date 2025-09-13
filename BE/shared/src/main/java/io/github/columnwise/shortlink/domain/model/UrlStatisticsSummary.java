package io.github.columnwise.shortlink.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@Schema(description = "URL 통계 요약")
public record UrlStatisticsSummary(
        @Schema(description = "단축 코드", example = "abc123")
        String code,
        
        @Schema(description = "총 접속 횟수", example = "150")
        long totalAccessCount,
        
        @Schema(description = "오늘 접속 횟수", example = "25")
        long todayAccessCount,
        
        @Schema(description = "이번 주 접속 횟수", example = "80")
        long weeklyAccessCount,
        
        @Schema(description = "이번 달 접속 횟수", example = "120")
        long monthlyAccessCount,
        
        @Schema(description = "마지막 접속 시간", example = "2024-01-01T12:30:00Z")
        Instant lastAccessedAt,
        
        @Schema(description = "생성 시간", example = "2024-01-01T00:00:00Z")
        Instant createdAt
) {
}