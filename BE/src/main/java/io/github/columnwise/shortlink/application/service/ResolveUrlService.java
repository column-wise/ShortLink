package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResolveUrlService implements ResolveUrlUseCase {
    
    private final ShortUrlRepositoryPort shortUrlRepository;
    
    @Override
    public String resolveUrl(String code) {
        ShortUrl shortUrl = shortUrlRepository.findByCode(code)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for code: " + code));
                
        return shortUrl.longUrl();
    }
}
