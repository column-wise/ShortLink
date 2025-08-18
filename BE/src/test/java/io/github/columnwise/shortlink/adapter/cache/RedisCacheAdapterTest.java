package io.github.columnwise.shortlink.adapter.cache;

import io.github.columnwise.shortlink.config.RedisProperties;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheAdapterTest {

    @Mock
    private RedisTemplate<String, ShortUrl> redisTemplate;

    @Mock
    private ValueOperations<String, ShortUrl> valueOperations;

    @Mock
    private RedisProperties redisProperties;

    private RedisCacheAdapter cacheAdapter;

    @BeforeEach
    void setUp() {
        // Mock Redis properties
        RedisProperties.Cache cacheConfig = new RedisProperties.Cache();
        when(redisProperties.getCache()).thenReturn(cacheConfig);
        
        cacheAdapter = new RedisCacheAdapter(redisTemplate, redisProperties);
    }

    @Test
    @DisplayName("캐시에서 코드로 조회 성공")
    void findByCode_Success() {
        // Given
        String code = "abc123";
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .code(code)
                .longUrl("https://www.example.com")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("shorturl:" + code)).thenReturn(shortUrl);

        // When
        Optional<ShortUrl> result = cacheAdapter.findByCode(code);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(shortUrl);
        verify(valueOperations).get("shorturl:" + code);
    }

    @Test
    @DisplayName("캐시에 없는 코드 조회 시 Optional.empty 반환")
    void findByCode_NotFound() {
        // Given
        String code = "notfound";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("shorturl:" + code)).thenReturn(null);

        // When
        Optional<ShortUrl> result = cacheAdapter.findByCode(code);

        // Then
        assertThat(result).isEmpty();
        verify(valueOperations).get("shorturl:" + code);
    }

    @Test
    @DisplayName("캐시에 ShortUrl 저장 성공")
    void save_Success() {
        // Given
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .code("abc123")
                .longUrl("https://www.example.com")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        cacheAdapter.save(shortUrl);

        // Then
        verify(valueOperations).set(eq("shorturl:abc123"), eq(shortUrl), any(Duration.class));
    }

    @Test
    @DisplayName("캐시에서 코드 삭제 성공")
    void delete_Success() {
        // Given
        String code = "abc123";

        // When
        cacheAdapter.delete(code);

        // Then
        verify(redisTemplate).delete("shorturl:" + code);
    }

    @Test
    @DisplayName("캐시 만료 시간 설정 성공")
    void setExpiration_Success() {
        // Given
        String code = "abc123";
        long seconds = 3600;

        // When
        cacheAdapter.setExpiration(code, seconds);

        // Then
        verify(redisTemplate).expire("shorturl:" + code, Duration.ofSeconds(seconds));
    }
}