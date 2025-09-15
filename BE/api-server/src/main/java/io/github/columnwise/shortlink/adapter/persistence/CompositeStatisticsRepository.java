package io.github.columnwise.shortlink.adapter.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.columnwise.shortlink.application.port.out.StatisticsRepository;
import io.github.columnwise.shortlink.domain.model.DailyStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CompositeStatisticsRepository implements StatisticsRepository {
    
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public List<DailyStatistics> getDailyStatistics(String code, LocalDate startDate, LocalDate endDate) {
        // batch-server가 만든 통계 캐시에서 조회
        List<DailyStatistics> result = getBatchProcessedStatistics(code, startDate, endDate);

        // 캐시가 없으면 빈 리스트 반환 (실시간 계산 안함)
        return result != null ? result : new ArrayList<>();
    }

    /**
     * batch-server가 처리한 통계 캐시를 조회합니다.
     *
     * <p>batch-server가 주기적으로 원시 방문 데이터를 집계해서 만든 통계 캐시를 조회합니다.
     * "url:daily:stats:" 키 패턴으로 저장된 일별 통계를 날짜 범위에 따라 조회합니다.</p>
     *
     * @param code 단축 코드
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return batch-server가 처리한 통계 리스트 (없으면 null)
     */
    private List<DailyStatistics> getBatchProcessedStatistics(String code, LocalDate startDate, LocalDate endDate) {
        List<DailyStatistics> result = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            String dateKey = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String statsKey = "url:daily:stats:" + code + ":" + dateKey;

            try {
                Map<Object, Object> dailyStats = objectRedisTemplate.opsForHash().entries(statsKey);
                if (dailyStats != null && !dailyStats.isEmpty()) {
                    Long accessCount = getLongValue(dailyStats.get("accessCount"));
                    Long uniqueVisitors = getLongValue(dailyStats.get("uniqueVisitors"));

                    if (accessCount != null && accessCount > 0) {
                        result.add(DailyStatistics.builder()
                                .code(code)
                                .date(currentDate)
                                .accessCount(accessCount)
                                .uniqueVisitors(uniqueVisitors != null ? uniqueVisitors : 0)
                                .build());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get batch processed statistics for key: {}", statsKey, e);
            }

            currentDate = currentDate.plusDays(1);
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Object 값을 Long으로 안전하게 변환합니다.
     */
    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    
    @Override
    public long getAccessCountForDate(String code, LocalDate date) {
        // batch-server가 처리한 일별 통계에서 조회
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String statsKey = "url:daily:stats:" + code + ":" + dateKey;

        try {
            Map<Object, Object> dailyStats = objectRedisTemplate.opsForHash().entries(statsKey);
            if (dailyStats != null && !dailyStats.isEmpty()) {
                Long accessCount = getLongValue(dailyStats.get("accessCount"));
                return accessCount != null ? accessCount : 0;
            }
        } catch (Exception e) {
            log.warn("Failed to get access count for date {} from batch statistics key: {}", date, statsKey, e);
        }

        return 0;
    }
}