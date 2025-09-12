package io.github.columnwise.shortlink.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@Schema(description = "URL 통계 정보")
public record UrlStatistics(
        @Schema(description = "통계 ID", example = "1")
        long id,
        
        @Schema(description = "단축 코드", example = "abc123")
        String code,
        
        @Schema(description = "총 접속 횟수", example = "150")
        long totalAccess,
        
        @Schema(description = "일별 접속 횟수", example = "25")
        long dailyAccess,
        
        @Schema(description = "통계 날짜", example = "2024-01-01T00:00:00Z")
        Instant statisticsDate,
        
        @Schema(description = "마지막 접속 시간", example = "2024-01-01T12:30:00Z")
        Instant lastAccessedAt,
        
        @Schema(description = "생성 시간", example = "2024-01-01T00:00:00Z")
        Instant createdAt,
        
        @Schema(description = "수정 시간", example = "2024-01-01T12:30:00Z")
        Instant updatedAt
) {
}