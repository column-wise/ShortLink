package io.github.columnwise.shortlink.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "url_statistics")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlStatisticsEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 10)
    private String code;
    
    @Column(nullable = false)
    @Builder.Default
    private Long totalAccessCount = 0L;
    
    private Instant lastAccessedAt;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    
    public void incrementAccessCount(long increment) {
        this.totalAccessCount = (this.totalAccessCount != null ? this.totalAccessCount : 0) + increment;
    }
    
    public void updateLastAccessTime(Instant accessTime) {
        this.lastAccessedAt = accessTime;
    }
}