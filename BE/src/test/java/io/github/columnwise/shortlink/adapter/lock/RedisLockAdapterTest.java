package io.github.columnwise.shortlink.adapter.lock;

import io.github.columnwise.shortlink.config.RedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisLockAdapterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RedisProperties redisProperties;

    private RedisLockAdapter lockAdapter;

    @BeforeEach
    void setUp() {
        // Mock Redis properties
        RedisProperties.Lock lockConfig = new RedisProperties.Lock();
        when(redisProperties.getLock()).thenReturn(lockConfig);
        
        lockAdapter = new RedisLockAdapter(redisTemplate, redisProperties);
    }

    @Test
    @DisplayName("분산 락 획득 성공")
    void tryLock_Success() {
        // Given
        String key = "test-lock";
        Duration expiration = Duration.ofSeconds(30);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:" + key), anyString(), eq(expiration)))
                .thenReturn(true);

        // When
        boolean result = lockAdapter.tryLock(key, expiration);

        // Then
        assertThat(result).isTrue();
        verify(valueOperations).setIfAbsent(eq("lock:" + key), anyString(), eq(expiration));
    }

    @Test
    @DisplayName("분산 락 획득 실패 - 이미 락이 존재")
    void tryLock_Failed_LockExists() {
        // Given
        String key = "test-lock";
        Duration expiration = Duration.ofSeconds(30);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:" + key), anyString(), eq(expiration)))
                .thenReturn(false);

        // When
        boolean result = lockAdapter.tryLock(key, expiration);

        // Then
        assertThat(result).isFalse();
        verify(valueOperations).setIfAbsent(eq("lock:" + key), anyString(), eq(expiration));
    }

    @Test
    @DisplayName("분산 락 획득 실패 - Redis 예외")
    void tryLock_Failed_Exception() {
        // Given
        String key = "test-lock";
        Duration expiration = Duration.ofSeconds(30);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:" + key), anyString(), eq(expiration)))
                .thenThrow(new RuntimeException("Redis error"));

        // When
        boolean result = lockAdapter.tryLock(key, expiration);

        // Then
        assertThat(result).isFalse();
        verify(valueOperations).setIfAbsent(eq("lock:" + key), anyString(), eq(expiration));
    }

    @Test
    @DisplayName("락 존재 여부 확인 - 존재하는 경우")
    void isLocked_True() {
        // Given
        String key = "test-lock";
        when(redisTemplate.hasKey("lock:" + key)).thenReturn(true);

        // When
        boolean result = lockAdapter.isLocked(key);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey("lock:" + key);
    }

    @Test
    @DisplayName("락 존재 여부 확인 - 존재하지 않는 경우")
    void isLocked_False() {
        // Given
        String key = "test-lock";
        when(redisTemplate.hasKey("lock:" + key)).thenReturn(false);

        // When
        boolean result = lockAdapter.isLocked(key);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey("lock:" + key);
    }

    @Test
    @DisplayName("락 존재 여부 확인 시 예외 발생하면 false 반환")
    void isLocked_Exception_ReturnsFalse() {
        // Given
        String key = "test-lock";
        when(redisTemplate.hasKey("lock:" + key)).thenThrow(new RuntimeException("Redis error"));

        // When
        boolean result = lockAdapter.isLocked(key);

        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey("lock:" + key);
    }
}