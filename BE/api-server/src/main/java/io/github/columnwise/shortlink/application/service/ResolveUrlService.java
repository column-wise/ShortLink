package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import org.springframework.data.redis.core.RedisTemplate;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ResolveUrlService implements ResolveUrlUseCase {
    
    private final ShortUrlRepositoryPort shortUrlRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public String resolveUrl(String code) {
        ShortUrl shortUrl = shortUrlRepository.findByCode(code)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for code: " + code));
        
        // Redis에 타임스탬프 기반 방문 기록 저장
        recordVisit(code);
        
        return shortUrl.longUrl();
    }
    
    private void recordVisit(String code) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String key = "url:access:count:" + code + ":" + timestamp;
        
        // 해당 키에 값을 설정 (값은 1로 고정, 키 존재 자체가 방문을 의미)
        redisTemplate.opsForValue().set(key, "1");
    }
}
