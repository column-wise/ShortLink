package io.github.columnwise.shortlink.adapter.redis;

import io.github.columnwise.shortlink.application.port.out.StatisticsWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class StatisticsWriterAdapter implements StatisticsWriter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DAILY_STATS_KEY_PREFIX = "url:daily:stats:";
    private static final String PROCESSED_KEY_PREFIX = "url:processed:";

    @Override
    public void saveDailyStatistics(String code, LocalDate date, long accessCount, 
                                   long expireTime, TimeUnit expireUnit) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dailyStatsKey = DAILY_STATS_KEY_PREFIX + code + ":" + dateKey;
        
        redisTemplate.opsForHash().put(dailyStatsKey, "accessCount", String.valueOf(accessCount));
        redisTemplate.opsForHash().put(dailyStatsKey, "date", dateKey);
        redisTemplate.opsForHash().put(dailyStatsKey, "processedAt", Instant.now().toString());
        
        // 만료 시간 설정
        redisTemplate.expire(dailyStatsKey, expireTime, expireUnit);
    }

    @Override
    public void markAsProcessed(String code, LocalDate date, long expireTime, TimeUnit expireUnit) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String processedKey = PROCESSED_KEY_PREFIX + code + ":" + dateKey;
        
        redisTemplate.opsForValue().set(processedKey, "true", expireTime, expireUnit);
    }
}