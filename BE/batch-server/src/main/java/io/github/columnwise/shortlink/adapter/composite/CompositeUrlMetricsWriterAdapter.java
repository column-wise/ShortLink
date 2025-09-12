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
        boolean dbSuccess = false;
        boolean redisSuccess = false;
        long dbResult = 0;
        long redisResult = 0;
        Exception dbException = null;
        Exception redisException = null;
        
        // DB 업데이트 시도
        try {
            dbResult = databaseWriter.incrementTotalAccessCount(code, increment);
            dbSuccess = true;
            log.debug("Database access count updated for code: {}, new total: {}", code, dbResult);
        } catch (Exception e) {
            dbException = e;
            log.error("Failed to update database access count for code: {}", code, e);
        }
        
        // Redis 업데이트 시도
        try {
            redisResult = redisWriter.incrementTotalAccessCount(code, increment);
            redisSuccess = true;
            log.debug("Redis access count updated for code: {}, increment: {}", code, increment);
        } catch (Exception e) {
            redisException = e;
            log.error("Failed to update Redis access count for code: {}", code, e);
        }
        
        // 둘 다 실패한 경우 예외 발생
        if (!dbSuccess && !redisSuccess) {
            String errorMessage = String.format("Both DB and Redis writes failed for code: %s", code);
            log.error(errorMessage);
            
            // 복합 예외 메시지 생성
            StringBuilder detailMessage = new StringBuilder(errorMessage);
            if (dbException != null) {
                detailMessage.append(" | DB Error: ").append(dbException.getMessage());
            }
            if (redisException != null) {
                detailMessage.append(" | Redis Error: ").append(redisException.getMessage());
            }
            
            throw new StatisticsUpdateException(detailMessage.toString());
        }
        
        // DB가 성공했으면 DB 결과를, 실패했으면 Redis 결과를 반환
        long result = dbSuccess ? dbResult : redisResult;
        
        if (!dbSuccess || !redisSuccess) {
            log.warn("Partial failure in composite write for code: {} - DB success: {}, Redis success: {}", 
                    code, dbSuccess, redisSuccess);
        }
        
        return result;
    }

    @Override
    public void updateLastAccessTime(String code, long timestamp) {
        boolean dbSuccess = false;
        boolean redisSuccess = false;
        Exception dbException = null;
        Exception redisException = null;
        
        // DB 업데이트 시도
        try {
            databaseWriter.updateLastAccessTime(code, timestamp);
            dbSuccess = true;
            log.debug("Database last access time updated for code: {}", code);
        } catch (Exception e) {
            dbException = e;
            log.error("Failed to update database last access time for code: {}", code, e);
        }
        
        // Redis 업데이트 시도
        try {
            redisWriter.updateLastAccessTime(code, timestamp);
            redisSuccess = true;
            log.debug("Redis last access time updated for code: {}", code);
        } catch (Exception e) {
            redisException = e;
            log.error("Failed to update Redis last access time for code: {}", code, e);
        }
        
        // 둘 다 실패한 경우 예외 발생
        if (!dbSuccess && !redisSuccess) {
            String errorMessage = String.format("Both DB and Redis last access time updates failed for code: %s", code);
            log.error(errorMessage);
            
            // 복합 예외 메시지 생성
            StringBuilder detailMessage = new StringBuilder(errorMessage);
            if (dbException != null) {
                detailMessage.append(" | DB Error: ").append(dbException.getMessage());
            }
            if (redisException != null) {
                detailMessage.append(" | Redis Error: ").append(redisException.getMessage());
            }
            
            throw new StatisticsUpdateException(detailMessage.toString());
        }
        
        if (!dbSuccess || !redisSuccess) {
            log.warn("Partial failure in last access time update for code: {} - DB success: {}, Redis success: {}", 
                    code, dbSuccess, redisSuccess);
        }
    }
}