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

    // 해시된 방문자 키
    @Column(length = 64)
    private String visitorHash;

    // chrome, safari 등
    @Column(length = 64)
    private String uaFamily;

    // mobile/desktop/tablet 등
    @Column(length = 32)
    private String deviceType;  

    // 방문자가 어느 사이트에서 왔는지 추적
    @Column(length = 128)
    private String referrerDomain;

    @Column(nullable = false)
    private Instant accessedAt; // DB는 timestamptz 권장
}
