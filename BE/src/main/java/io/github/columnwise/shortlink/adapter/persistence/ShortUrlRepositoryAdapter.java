package io.github.columnwise.shortlink.adapter.persistence;

import io.github.columnwise.shortlink.adapter.persistence.entity.ShortUrlEntity;
import io.github.columnwise.shortlink.adapter.persistence.entity.UrlAccessLogEntity;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.model.UrlAccessLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ShortUrlRepositoryAdapter implements ShortUrlRepositoryPort {
    
    private final SpringDataShortUrlRepository shortUrlRepository;
    private final SpringDataUrlAccessLogRepository accessLogRepository;
    
    @Override
    public ShortUrl save(ShortUrl shortUrl) {
        ShortUrlEntity entity = ShortUrlEntity.builder()
                .code(shortUrl.code())
                .longUrl(shortUrl.longUrl())
                .createdAt(shortUrl.createdAt())
                .expiresAt(shortUrl.expiresAt())
                .build();
                
        ShortUrlEntity saved = shortUrlRepository.save(entity);
        
        return ShortUrl.builder()
                .id(saved.getId())
                .code(saved.getCode())
                .longUrl(saved.getLongUrl())
                .createdAt(saved.getCreatedAt())
                .expiresAt(saved.getExpiresAt())
                .build();
    }
    
    @Override
    public Optional<ShortUrl> findByCode(String code) {
        return shortUrlRepository.findByCode(code)
                .map(entity -> ShortUrl.builder()
                        .id(entity.getId())
                        .code(entity.getCode())
                        .longUrl(entity.getLongUrl())
                        .createdAt(entity.getCreatedAt())
                        .expiresAt(entity.getExpiresAt())
                        .build());
    }
    
    @Override
    public void saveAccessLog(UrlAccessLog accessLog) {
        UrlAccessLogEntity entity = UrlAccessLogEntity.builder()
                .code(accessLog.code())
                .ipAddress(accessLog.ipAddress())
                .userAgent(accessLog.userAgent())
                .accessedAt(accessLog.accessedAt())
                .build();
                
        accessLogRepository.save(entity);
    }
    
    @Override
    public Optional<ShortUrl> findByLongUrl(String longUrl) {
        return shortUrlRepository.findByLongUrl(longUrl)
                .map(entity -> ShortUrl.builder()
                        .id(entity.getId())
                        .code(entity.getCode())
                        .longUrl(entity.getLongUrl())
                        .createdAt(entity.getCreatedAt())
                        .expiresAt(entity.getExpiresAt())
                        .build());
    }
    
    @Override
    public List<UrlAccessLog> findAccessLogsByCode(String code) {
        return accessLogRepository.findByCodeOrderByAccessedAtDesc(code)
                .stream()
                .map(entity -> UrlAccessLog.builder()
                        .id(entity.getId())
                        .code(entity.getCode())
                        .ipAddress(entity.getIpAddress())
                        .userAgent(entity.getUserAgent())
                        .accessedAt(entity.getAccessedAt())
                        .build())
                .toList();
    }
}
