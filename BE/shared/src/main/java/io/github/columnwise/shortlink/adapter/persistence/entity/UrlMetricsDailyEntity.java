package io.github.columnwise.shortlink.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일별 URL 통계 엔티티
 *
 * API 서버와 배치 서버 모두에서 사용되는 공용 엔티티입니다.
 * - API: /metrics 엔드포인트에서 통계 조회
 * - Batch: 시간별 데이터를 일별로 집계하여 저장
 */
@Entity
@Table(name = "url_metrics_daily",
    uniqueConstraints = @UniqueConstraint(columnNames = {"code", "day"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UrlMetricsDailyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 단축 코드
     */
    @Column(name = "code", length = 10, nullable = false)
    private String code;

    /**
     * 통계 날짜 (KST 기준)
     */
    @Column(name = "day", nullable = false)
    private LocalDate day;

    /**
     * 해당 일의 총 접근 수 (절대값)
     */
    @Column(name = "accesses", nullable = false)
    private Long accesses;

    /**
     * 해당 일의 유니크 방문자 수 (HLL PFCOUNT 결과)
     */
    @Column(name = "unique_visitors", nullable = false)
    private Long uniqueVisitors;

    /**
     * 디바이스별 접근 수 JSON
     * 예: {"mobile":123, "desktop":45, "tablet":12}
     */
    @Column(name = "device_accesses_json", nullable = false, columnDefinition = "JSON")
    private String deviceAccessesJson;


    /**
     * 업데이트 시각
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}