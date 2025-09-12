package io.github.columnwise.shortlink.adapter.redis;

import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriter;
import io.github.columnwise.shortlink.domain.service.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component("clusterRedisUrlMetricsWriter")
@RequiredArgsConstructor
public class ClusterRedisUrlMetricsWriterAdapter implements UrlMetricsWriter {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public long incrementTotalAccessCount(String code, long increment) {
        LocalDate today = LocalDate.now();
        String totalAccessKey = RedisKeyManager.getTotalAccessKey(code, today);
        
        Long result = redisTemplate.opsForValue().increment(totalAccessKey, increment);
        long newTotal = result != null ? result : increment;
        
        log.debug("Incremented total access count for code {}: +{} = {}", 
                code, increment, newTotal);
        
        return newTotal;
    }

    @Override
    public void updateLastAccessTime(String code, long timestamp) {
        LocalDate today = LocalDate.now();
        String lastAccessKey = RedisKeyManager.getLastAccessKey(code, today);
        
        redisTemplate.opsForValue().set(lastAccessKey, String.valueOf(timestamp));
        
        log.debug("Updated last access time for code {}: {}", code, timestamp);
    }
}