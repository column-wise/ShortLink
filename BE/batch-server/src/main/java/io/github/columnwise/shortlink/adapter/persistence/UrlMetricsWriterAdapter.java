package io.github.columnwise.shortlink.adapter.persistence;

import io.github.columnwise.shortlink.adapter.persistence.entity.UrlMetricsDailyEntity;
import io.github.columnwise.shortlink.adapter.persistence.entity.UrlMetricsHourlyEntity;
import io.github.columnwise.shortlink.adapter.persistence.repository.UrlMetricsDailyRepository;
import io.github.columnwise.shortlink.adapter.persistence.repository.UrlMetricsHourlyRepository;
import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UrlMetricsWriterAdapter implements UrlMetricsWriterPort {

    private final UrlMetricsHourlyRepository hourlyRepository;
    private final UrlMetricsDailyRepository dailyRepository;
    private final JdbcTemplate jdbcTemplate;
    @Override
    @Transactional
    public UrlMetricsHourlyEntity saveHourlyMetrics(UrlMetricsHourlyEntity entity) {
        return hourlyRepository.save(entity);
    }

    @Override
    @Transactional
    public int saveHourlyMetricsBatch(List<UrlMetricsHourlyEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

		final Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());
		int batchSize = Math.min(entities.size(), 1000);
        String sql = """
            INSERT INTO url_metrics_hourly
            (code, day, hour, accesses, unique_visitors, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            accesses = VALUES(accesses),
            unique_visitors = VALUES(unique_visitors),
            updated_at = VALUES(updated_at)
            """;

        try {
            int[][] results = jdbcTemplate.batchUpdate(sql, entities, batchSize,
                (PreparedStatement ps, UrlMetricsHourlyEntity entity) -> {
                    ps.setString(1, entity.getCode());
                    ps.setObject(2, java.sql.Date.valueOf(entity.getDay()));
                    ps.setShort(3, entity.getHour());
                    ps.setLong(4, entity.getAccesses());
					ps.setLong(5, entity.getUniqueVisitors());
                    ps.setTimestamp(6, nowTs);
                });

			int totalInserted = 0;
			boolean hasNoInfo = false;

			for (int[] batch : results) {
				for (int r : batch) {
					if (r == java.sql.Statement.SUCCESS_NO_INFO) {
						hasNoInfo = true;
					} else if (r == java.sql.Statement.EXECUTE_FAILED) {
						// 실패 건은 0으로 취급(필요 시 예외로 전환 가능)
					} else if (r > 0) {
						totalInserted += r;
					}
				}
			}

			if (hasNoInfo) {
				// 드라이버가 건수 정보를 주지 않은 경우: 실행된 row 개수로 환산
				totalInserted = java.util.Arrays.stream(results).mapToInt(a -> a.length).sum();
			}

			log.info("Successfully bulk inserted {} hourly metrics entities", totalInserted);
			return totalInserted;

		} catch (Exception e) {
			log.error("Failed to bulk insert hourly metrics batch", e);
			throw e;
		}
    }

    @Override
    @Transactional
    public UrlMetricsDailyEntity saveDailyMetrics(UrlMetricsDailyEntity entity) {
        return dailyRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UrlMetricsDailyEntity> findDailyMetrics(String code, LocalDate date) {
        return dailyRepository.findByCodeAndDay(code, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UrlMetricsHourlyEntity> findHourlyMetricsByDate(String code, LocalDate date) {
        return hourlyRepository.findByCodeAndDay(code, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UrlMetricsHourlyEntity> findAllHourlyMetricsByDate(LocalDate date) {
        return hourlyRepository.findByDay(date);
    }

    @Override
    @Transactional(readOnly = true)
    public long sumHourlyAccesses(String code, LocalDate date) {
        return hourlyRepository.sumAccessesByCodeAndDay(code, date);
    }
}
