package io.github.columnwise.shortlink.adapter.redis;

import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisUrlMetricsWriterAdapter implements UrlMetricsWriter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String URL_TOTAL_ACCESS_KEY_PREFIX = "url:total:access:";
    private static final String URL_LAST_ACCESS_KEY_PREFIX = "url:last:access:";

    @Override
    public long incrementTotalAccessCount(String code, long increment) {
        String totalAccessKey = URL_TOTAL_ACCESS_KEY_PREFIX + code;
        Long result = redisTemplate.opsForValue().increment(totalAccessKey, increment);
        return result != null ? result : increment;
    }

    @Override
    public void updateLastAccessTime(String code, long timestamp) {
        String lastAccessKey = URL_LAST_ACCESS_KEY_PREFIX + code;
        redisTemplate.opsForValue().set(lastAccessKey, String.valueOf(timestamp));
    }
}