package io.github.columnwise.shortlink.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 배치 워터마크 엔티티 (배치 서버 전용)
 *
 * 각 단축 코드별로 마지막으로 처리한 시간 버킷을 추적하여
 * 배치 재실행 시 중복 처리를 방지하고 증분 처리를 가능하게 합니다.
 */
@Entity
@Table(name = "batch_watermark")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BatchWatermarkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 단축 코드
     */
    @Column(name = "code", length = 10, nullable = false, unique = true)
    private String code;

    /**
     * 마지막으로 확정 반영한 시간 버킷의 종료 시각
     *
     * 예: 2024-01-01 15:00:00 이라면
     * 2024-01-01 14:00~15:00 시간 버킷까지 처리 완료를 의미
     */
    @Column(name = "last_processed_utc", nullable = false)
    private LocalDateTime lastProcessedUtc;

    /**
     * 워터마크 업데이트 시각
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주어진 시간이 이미 처리된 시간인지 확인
     */
    public boolean isAlreadyProcessed(LocalDateTime targetTime) {
        return targetTime.isBefore(lastProcessedUtc) || targetTime.isEqual(lastProcessedUtc);
    }

    /**
     * 워터마크를 주어진 시간으로 업데이트
     */
    public void updateWatermark(LocalDateTime processedTime) {
        this.lastProcessedUtc = processedTime;
    }
}