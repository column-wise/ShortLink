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

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    
    @Test
    @DisplayName("null 키로 락 획득 시도 시 false 반환")
    void tryLock_NullKey_ReturnsFalse() {
        // When
        boolean result = lockAdapter.tryLock(null, Duration.ofSeconds(30));

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("빈 키로 락 획득 시도 시 false 반환")
    void tryLock_EmptyKey_ReturnsFalse() {
        // When
        boolean result = lockAdapter.tryLock("", Duration.ofSeconds(30));

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("공백 키로 락 획득 시도 시 false 반환")
    void tryLock_WhitespaceKey_ReturnsFalse() {
        // When
        boolean result = lockAdapter.tryLock("   ", Duration.ofSeconds(30));

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("null Duration으로 락 획득 시도 시 false 반환")
    void tryLock_NullDuration_ReturnsFalse() {
        // When
        boolean result = lockAdapter.tryLock("test-lock", null);

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("음수 Duration으로 락 획득 시도 시 false 반환")
    void tryLock_NegativeDuration_ReturnsFalse() {
        // When
        boolean result = lockAdapter.tryLock("test-lock", Duration.ofSeconds(-1));

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("0 Duration으로 락 획득 시도 시 false 반환")
    void tryLock_ZeroDuration_ReturnsFalse() {
        // When
        boolean result = lockAdapter.tryLock("test-lock", Duration.ZERO);

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("null 키로 락 해제 시 무시됨")
    void unlock_NullKey_DoesNothing() {
        // When
        lockAdapter.unlock(null);

        // Then
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("빈 키로 락 해제 시 무시됨")
    void unlock_EmptyKey_DoesNothing() {
        // When
        lockAdapter.unlock("");

        // Then
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("null 키로 락 존재 여부 확인 시 false 반환")
    void isLocked_NullKey_ReturnsFalse() {
        // When
        boolean result = lockAdapter.isLocked(null);

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("빈 키로 락 존재 여부 확인 시 false 반환")
    void isLocked_EmptyKey_ReturnsFalse() {
        // When
        boolean result = lockAdapter.isLocked("");

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(redisTemplate);
    }
}