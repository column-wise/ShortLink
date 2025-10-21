package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.ProcessHourlyStatisticsUseCase;
import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReaderPort;
import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriterPort;
import io.github.columnwise.shortlink.adapter.persistence.entity.UrlMetricsHourlyEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessHourlyStatisticsService implements ProcessHourlyStatisticsUseCase {

	private final RedisStatisticsReaderPort redisStatisticsReaderPort;
	private final UrlMetricsWriterPort urlMetricsWriterPort;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public int processPreviousHour() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime previousHour = now.minusHours(1);
		LocalDate targetDate = previousHour.toLocalDate();
		int targetHour = previousHour.getHour();

		log.info("Processing current hour statistics for date: {} hour: {}", targetDate, targetHour);

		// Redis에서 해당 날짜에 통계가 있는 모든 코드를 찾기
		var codes = redisStatisticsReaderPort.findCodesWithStatistics(targetDate);
		if (codes.isEmpty()) {
			log.info("No codes found with statistics for date: {}", targetDate);
			return 0;
		}

		log.info("Found {} codes with statistics for date: {}", codes.size(), targetDate);

		List<UrlMetricsHourlyEntity> entities = new ArrayList<>();
		int processedCount = 0;

		for (String code : codes) {
			try {
				// Redis에서 시간별 접근 통계 읽기 (특정 시간만)
				Map<String, Long> hourlyAccesses = redisStatisticsReaderPort.getHourlyAccesses(code, targetDate);
				if (hourlyAccesses == null || !hourlyAccesses.containsKey(String.format("%02d", targetHour))) {
					continue; // 해당 시간에 데이터가 없으면 스킵
				}

				Long accesses = hourlyAccesses.get(String.format("%02d", targetHour));
				if (accesses == null || accesses <= 0) {
					continue;
				}

				// UV는 일별 값을 그대로 사용
				Long uniqueVisitors = redisStatisticsReaderPort.getDailyUniqueVisitorsCount(code, targetDate);

				UrlMetricsHourlyEntity entity = UrlMetricsHourlyEntity.builder()
					.code(code)
					.day(targetDate)
					.hour((short) targetHour)
					.accesses(accesses)
					.uniqueVisitors(uniqueVisitors != null ? uniqueVisitors : 0L)
					.build();

				entities.add(entity);
				processedCount++;
			} catch (Exception e) {
				log.error("Failed to process code: {} for date: {} hour: {}", code, targetDate, targetHour, e);
			}
		}

		// DB에 배치 저장
		if (!entities.isEmpty()) {
			int savedCount = urlMetricsWriterPort.saveHourlyMetricsBatch(entities);
			log.info("Saved {} hourly metrics entities for date: {} hour: {}", savedCount, targetDate, targetHour);
		}

		log.info("Successfully processed {}/{} codes for date: {} hour: {}", processedCount, codes.size(), targetDate, targetHour);
		return processedCount;
	}

	private String convertToJson(Map<String, Long> map) {
		try {
			return objectMapper.writeValueAsString(map);
		} catch (Exception e) {
			log.error("Failed to convert map to JSON: {}", map, e);
			return "{}";
		}
	}

	private Short parseHourSafely(String hourStr) {
		if (hourStr == null || hourStr.trim().isEmpty()) {
			return null;
		}

		try {
			short hour = Short.parseShort(hourStr.trim());
			if (hour < 0 || hour > 23) {
				return null;
			}
			return hour;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
