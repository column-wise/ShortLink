package io.github.columnwise.shortlink.adapter.redis;

import io.github.columnwise.shortlink.application.port.out.StatisticsWriter;
import io.github.columnwise.shortlink.domain.service.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class ClusterStatisticsWriterAdapter implements StatisticsWriter {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void saveDailyStatistics(String code, LocalDate date, long accessCount, 
                                   long expireTime, TimeUnit expireUnit) {
        // 일일 통계 저장
        String dailyStatsKey = RedisKeyManager.getDailyStatsKey(code, date);
        
        redisTemplate.opsForHash().put(dailyStatsKey, "accessCount", String.valueOf(accessCount));
        redisTemplate.opsForHash().put(dailyStatsKey, "date", date.toString());
        redisTemplate.opsForHash().put(dailyStatsKey, "processedAt", Instant.now().toString());
        
        // 만료 시간 설정
        redisTemplate.expire(dailyStatsKey, expireTime, expireUnit);
        
        // 코드를 일일 통계 코드 SET에 추가 (키 추적용)
        String dailyCodesSetKey = RedisKeyManager.getDailyCodesSetKey(date);
        redisTemplate.opsForSet().add(dailyCodesSetKey, code);
        redisTemplate.expire(dailyCodesSetKey, expireTime, expireUnit);
        
        log.debug("Saved daily statistics for code: {} on date: {}", code, date);
    }

    @Override
    public void markAsProcessed(String code, LocalDate date, long expireTime, TimeUnit expireUnit) {
        String processedKey = RedisKeyManager.getProcessedMarkerKey(code, date);
        redisTemplate.opsForValue().set(processedKey, "true", expireTime, expireUnit);
        
        log.debug("Marked as processed: code={}, date={}", code, date);
    }
}