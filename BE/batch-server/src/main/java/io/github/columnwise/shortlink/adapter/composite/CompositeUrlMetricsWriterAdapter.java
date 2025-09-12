package io.github.columnwise.shortlink.adapter.composite;

import io.github.columnwise.shortlink.adapter.persistence.DatabaseUrlMetricsWriterAdapter;
import io.github.columnwise.shortlink.adapter.redis.ClusterRedisUrlMetricsWriterAdapter;
import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class CompositeUrlMetricsWriterAdapter implements UrlMetricsWriter {

    private final ClusterRedisUrlMetricsWriterAdapter redisWriter;
    private final DatabaseUrlMetricsWriterAdapter databaseWriter;

    @Override
    public long incrementTotalAccessCount(String code, long increment) {
        long dbResult = 0;
        long redisResult = 0;
        
        try {
            // DB 업데이트 (메인 소스)
            dbResult = databaseWriter.incrementTotalAccessCount(code, increment);
            log.debug("Database access count updated for code: {}, new total: {}", code, dbResult);
        } catch (Exception e) {
            log.error("Failed to update database access count for code: {}", code, e);
            // DB 실패해도 Redis는 계속 시도
        }
        
        try {
            // Redis 업데이트 (캐시)
            redisResult = redisWriter.incrementTotalAccessCount(code, increment);
            log.debug("Redis access count updated for code: {}, increment: {}", code, increment);
        } catch (Exception e) {
            log.error("Failed to update Redis access count for code: {}", code, e);
        }
        
        // DB가 성공했으면 DB 결과를, 실패했으면 Redis 결과를 반환
        return dbResult > 0 ? dbResult : redisResult;
    }

    @Override
    public void updateLastAccessTime(String code, long timestamp) {
        try {
            // DB 업데이트
            databaseWriter.updateLastAccessTime(code, timestamp);
            log.debug("Database last access time updated for code: {}", code);
        } catch (Exception e) {
            log.error("Failed to update database last access time for code: {}", code, e);
        }
        
        try {
            // Redis 업데이트
            redisWriter.updateLastAccessTime(code, timestamp);
            log.debug("Redis last access time updated for code: {}", code);
        } catch (Exception e) {
            log.error("Failed to update Redis last access time for code: {}", code, e);
        }
    }
}