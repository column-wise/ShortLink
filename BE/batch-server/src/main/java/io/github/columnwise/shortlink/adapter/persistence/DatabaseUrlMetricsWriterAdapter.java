package io.github.columnwise.shortlink.adapter.persistence;

import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriter;
import io.github.columnwise.shortlink.domain.model.UrlStatisticsEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseUrlMetricsWriterAdapter implements UrlMetricsWriter {

    private final SpringDataUrlStatisticsRepository repository;

    @Override
    @Transactional
    public long incrementTotalAccessCount(String code, long increment) {
        Instant now = Instant.now();
        
        // 기존 레코드 업데이트 시도
        int updatedRows = repository.incrementAccessCount(code, increment, now);
        
        if (updatedRows == 0) {
            // 레코드가 없으면 새로 생성
            UrlStatisticsEntity newEntity = UrlStatisticsEntity.builder()
                    .code(code)
                    .totalAccessCount(increment)
                    .lastAccessedAt(now)
                    .build();
            
            UrlStatisticsEntity saved = repository.save(newEntity);
            log.debug("Created new statistics record for code: {}, count: {}", code, increment);
            return saved.getTotalAccessCount();
        } else {
            // 업데이트된 값을 조회해서 반환
            return repository.findByCode(code)
                    .map(UrlStatisticsEntity::getTotalAccessCount)
                    .orElse(increment);
        }
    }

    @Override
    @Transactional
    public void updateLastAccessTime(String code, long timestamp) {
        Instant accessTime = Instant.ofEpochMilli(timestamp);
        Instant now = Instant.now();
        
        int updatedRows = repository.updateLastAccessTime(code, accessTime, now);
        
        if (updatedRows == 0) {
            // 레코드가 없으면 새로 생성
            UrlStatisticsEntity newEntity = UrlStatisticsEntity.builder()
                    .code(code)
                    .totalAccessCount(0L)
                    .lastAccessedAt(accessTime)
                    .build();
            
            repository.save(newEntity);
            log.debug("Created new statistics record for code: {} with last access time", code);
        }
    }
}