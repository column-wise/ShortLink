package io.github.columnwise.shortlink.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

/**
 * 일별 접속 통계 도메인 모델
 *
 * <p>특정 단축 URL에 대한 일별 접속 수 및 고유 방문자 수를 나타내는 도메인 모델입니다.
 * REST API 응답으로도 사용되며, 프론트엔드에서 차트 등의 시각화에 활용됩니다.</p>
 *
 * <p>통계 정보:</p>
 * <ul>
 *   <li>accessCount: 해당 날짜의 총 접속 횟수</li>
 *   <li>uniqueVisitors: 고유 방문자 수 (기업 옵션에 따라 개략적 수치)</li>
 * </ul>
 *
 * @param code 대상 단축 코드
 * @param date 통계 날짜
 * @param accessCount 해당 날짜의 총 접속 횟수
 * @param uniqueVisitors 고유 방문자 수 (개략적)
 */
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