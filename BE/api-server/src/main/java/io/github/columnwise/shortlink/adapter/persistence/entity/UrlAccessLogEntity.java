package io.github.columnwise.shortlink.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "url_access_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlAccessLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 10)
    private String code;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(length = 512)
    private String userAgent;
    
    @Column(nullable = false)
    private Instant accessedAt;
}
