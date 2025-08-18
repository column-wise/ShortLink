package io.github.columnwise.shortlink.adapter.cache;

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

@ExtendWith(MockitoExtension.class)
class RedisHitCounterAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisHitCounterAdapter hitCounterAdapter;

    @BeforeEach
    void setUp() {
        hitCounterAdapter = new RedisHitCounterAdapter(redisTemplate);
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
        when(valueOperations.get("hitcount:" + code)).thenReturn(5L);

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
}