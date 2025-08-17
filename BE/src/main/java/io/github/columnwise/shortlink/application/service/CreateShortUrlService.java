package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.CreateShortUrlUseCase;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.service.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateShortUrlService implements CreateShortUrlUseCase {
    
    private final ShortUrlRepositoryPort shortUrlRepository;
    private final CodeGenerator codeGenerator;
    
    @Override
    public ShortUrl createShortUrl(String longUrl) {
        // 기존 URL이 있으면 반환
        Optional<ShortUrl> existing = shortUrlRepository.findByLongUrl(longUrl);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // 코드 충돌 방지를 위한 재시도 로직
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            String code = codeGenerator.generate(longUrl + "_" + i); // salt 추가
            
            // 코드 중복 확인
            if (shortUrlRepository.findByCode(code).isEmpty()) {
                ShortUrl shortUrl = ShortUrl.builder()
                        .code(code)
                        .longUrl(longUrl)
                        .createdAt(Instant.now())
                        .expiresAt(Instant.now().plus(365, ChronoUnit.DAYS))
                        .build();
                        
                try {
                    return shortUrlRepository.save(shortUrl);
                } catch (Exception e) {
                    // 동시성으로 인한 충돌 시 재시도
                    if (i == maxRetries - 1) {
                        throw new RuntimeException("Failed to generate unique code after " + maxRetries + " attempts", e);
                    }
                }
            }
        }
        
        throw new RuntimeException("Failed to generate unique code");
    }
}
