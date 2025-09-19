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
 * 시간별 URL 통계 엔티티 (배치 서버 전용)
 *
 * Redis에서 수집한 시간별 통계를 임시 저장하여 일별 집계의 중간 단계로 사용합니다.
 * 배치 처리가 완료되면 해당 데이터는 UrlMetricsDailyEntity로 집계됩니다.
 */
@Entity
@Table(name = "url_metrics_hourly",
       uniqueConstraints = @UniqueConstraint(columnNames = {"code", "day", "hour"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UrlMetricsHourlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 단축 코드
     */
    @Column(name = "code", length = 10, nullable = false)
    private String code;

    /**
     * 통계 날짜
     */
    @Column(name = "day", nullable = false)
    private LocalDate day;

    /**
     * 시간 (0~23)
     */
    @Column(name = "hour", nullable = false)
    private Short hour;

    /**
     * 해당 시간 버킷의 총 접근 수 (절대값)
     */
    @Column(name = "accesses", nullable = false)
    private Long accesses;

    @Column(name = "unique_visitors", nullable = false)
    private Long uniqueVisitors;

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