package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolveUrlServiceTest {

    @Mock
    private ShortUrlRepositoryPort shortUrlRepository;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ResolveUrlService resolveUrlService;

    @BeforeEach
    void setUp() {
        resolveUrlService = new ResolveUrlService(shortUrlRepository, redisTemplate);
    }

    @Test
    @DisplayName("유효한 코드로 URL 조회 성공")
    void resolveUrl_ValidCode_Success() {
        // Given
        String code = "abc123";
        String longUrl = "https://www.example.com";
        
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .code(code)
                .longUrl(longUrl)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(31536000))
                .build();

        when(shortUrlRepository.findByCode(code)).thenReturn(Optional.of(shortUrl));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        String result = resolveUrlService.resolveUrl(code);

        // Then
        assertThat(result).isEqualTo(longUrl);
        verify(shortUrlRepository).findByCode(code);
        verify(valueOperations).set(argThat(key -> key.startsWith("url:access:count:" + code + ":")), eq("1"));
    }

    @Test
    @DisplayName("존재하지 않는 코드로 조회 시 예외 발생")
    void resolveUrl_NonExistentCode_ThrowsException() {
        // Given
        String code = "notfound";

        when(shortUrlRepository.findByCode(code)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> resolveUrlService.resolveUrl(code))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("URL not found for code: " + code);
        
        verify(shortUrlRepository).findByCode(code);
        verifyNoInteractions(redisTemplate);
    }
}