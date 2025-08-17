package io.github.columnwise.shortlink.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "short_urls")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortUrlEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 10)
    private String code;
    
    @Column(nullable = false, length = 2048)
    private String longUrl;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    private Instant expiresAt;
}
