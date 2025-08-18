package io.github.columnwise.shortlink.adapter.cache;

import io.github.columnwise.shortlink.config.RedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisHitCounterAdapterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RedisProperties redisProperties;

    private RedisHitCounterAdapter hitCounterAdapter;

    @BeforeEach
    void setUp() {
        // Mock Redis properties
        RedisProperties.HitCounter hitCounterConfig = new RedisProperties.HitCounter();
        when(redisProperties.getHitCounter()).thenReturn(hitCounterConfig);
        
        hitCounterAdapter = new RedisHitCounterAdapter(redisTemplate, redisProperties);
    }

    @Test
    @DisplayName("조회수 증가 성공")
    void incrementHitCount_Success() {
        // Given
        String code = "abc123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("hitcount:" + code)).thenReturn(1L);

        // When
        hitCounterAdapter.incrementHitCount(code);

        // Then
        verify(valueOperations).increment("hitcount:" + code);
    }

    @Test
    @DisplayName("조회수 조회 성공 - 카운트가 있는 경우")
    void getHitCount_Success_WithCount() {
        // Given
        String code = "abc123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hitcount:" + code)).thenReturn("5");

        // When
        long result = hitCounterAdapter.getHitCount(code);

        // Then
        assertThat(result).isEqualTo(5L);
        verify(valueOperations).get("hitcount:" + code);
    }

    @Test
    @DisplayName("조회수 조회 성공 - 카운트가 없는 경우 0 반환")
    void getHitCount_Success_NoCount() {
        // Given
        String code = "abc123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hitcount:" + code)).thenReturn(null);

        // When
        long result = hitCounterAdapter.getHitCount(code);

        // Then
        assertThat(result).isEqualTo(0L);
        verify(valueOperations).get("hitcount:" + code);
    }

    @Test
    @DisplayName("조회수 초기화 성공")
    void resetHitCount_Success() {
        // Given
        String code = "abc123";

        // When
        hitCounterAdapter.resetHitCount(code);

        // Then
        verify(redisTemplate).delete("hitcount:" + code);
    }
    
    @Test
    @DisplayName("null 코드로 조회수 증가 시 무시됨")
    void incrementHitCount_NullCode_DoesNothing() {
        // When
        hitCounterAdapter.incrementHitCount(null);

        // Then
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("빈 코드로 조회수 증가 시 무시됨")
    void incrementHitCount_EmptyCode_DoesNothing() {
        // When
        hitCounterAdapter.incrementHitCount("");

        // Then
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("공백 코드로 조회수 증가 시 무시됨")
    void incrementHitCount_WhitespaceCode_DoesNothing() {
        // When
        hitCounterAdapter.incrementHitCount("   ");

        // Then
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("null 코드로 조회수 조회 시 0 반환")
    void getHitCount_NullCode_ReturnsZero() {
        // When
        long result = hitCounterAdapter.getHitCount(null);

        // Then
        assertThat(result).isEqualTo(0L);
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("빈 코드로 조회수 조회 시 0 반환")
    void getHitCount_EmptyCode_ReturnsZero() {
        // When
        long result = hitCounterAdapter.getHitCount("");

        // Then
        assertThat(result).isEqualTo(0L);
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("잘못된 숫자 형식의 조회수 처리")
    void getHitCount_InvalidNumberFormat_ReturnsZero() {
        // Given
        String code = "abc123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hitcount:" + code)).thenReturn("invalid_number");

        // When
        long result = hitCounterAdapter.getHitCount(code);

        // Then
        assertThat(result).isEqualTo(0L);
        verify(valueOperations).get("hitcount:" + code);
    }
    
    @Test
    @DisplayName("null 코드로 조회수 초기화 시 무시됨")
    void resetHitCount_NullCode_DoesNothing() {
        // When
        hitCounterAdapter.resetHitCount(null);

        // Then
        verifyNoInteractions(redisTemplate);
    }
    
    @Test
    @DisplayName("빈 코드로 조회수 초기화 시 무시됨")
    void resetHitCount_EmptyCode_DoesNothing() {
        // When
        hitCounterAdapter.resetHitCount("");

        // Then
        verifyNoInteractions(redisTemplate);
    }
}