package io.github.columnwise.shortlink.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "URL 통계 응답")
public record UrlStatistics(

        @Schema(description = "단축 코드", example = "abc123")
        String code,

        @Schema(description = "오늘 통계")
        TodayStats today,

        @Schema(description = "기간 통계")
        PeriodStats period
) {

        @Builder
        @Schema(description = "오늘 통계")
        public record TodayStats(
                @Schema(description = "시간대별 방문수 (0~23시)", example = "[12, 30, 22, ...]")
                List<Long> hourlyVisits,

                @Schema(description = "오늘 고유 방문자 수", example = "128")
                long uniqueVisitors,

                @Schema(description = "브라우저별 방문수", example = "{\"Chrome\": 50, \"Safari\": 30}")
                Map<String, Long> browserStats
        ) {}

        @Builder
        @Schema(description = "기간 통계")
        public record PeriodStats(
                @Schema(description = "시작 날짜", example = "2024-08-01")
                LocalDate from,

                @Schema(description = "끝 날짜", example = "2024-08-31")
                LocalDate to,

                @Schema(description = "일별 방문수")
                List<DailyStat> dailyStats
        ) {}

        @Builder
        @Schema(description = "일별 통계")
        public record DailyStat(
                @Schema(description = "날짜", example = "2024-08-01")
                LocalDate date,

                @Schema(description = "방문수", example = "120")
                long visits,

                @Schema(description = "고유 방문자 수", example = "85")
                long uniqueVisitors
        ) {}
}