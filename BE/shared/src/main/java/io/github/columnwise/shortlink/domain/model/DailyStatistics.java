package io.github.columnwise.shortlink.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "일별 접속 통계")
public record DailyStatistics(
        @Schema(description = "단축 코드", example = "abc123")
        String code,
        
        @Schema(description = "날짜", example = "2024-01-01")
        LocalDate date,
        
        @Schema(description = "접속 횟수", example = "25")
        long accessCount,
        
        @Schema(description = "고유 방문자 수 (개략적)", example = "18")
        long uniqueVisitors
) {
}