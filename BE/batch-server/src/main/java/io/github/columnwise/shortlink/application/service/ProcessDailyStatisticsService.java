package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.ProcessDailyStatisticsUseCase;
import io.github.columnwise.shortlink.application.port.out.UrlMetricsWriterPort;
import io.github.columnwise.shortlink.application.port.out.RedisStatisticsReaderPort;
import io.github.columnwise.shortlink.adapter.persistence.entity.UrlMetricsHourlyEntity;
import io.github.columnwise.shortlink.adapter.persistence.entity.UrlMetricsDailyEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessDailyStatisticsService implements ProcessDailyStatisticsUseCase {

	private final UrlMetricsWriterPort urlMetricsWriterPort;
	private final RedisStatisticsReaderPort redisStatisticsReaderPort;
	private final ObjectMapper objectMapper;

	@Autowired
	private ProcessDailyStatisticsService self;

	private static final int DEFAULT_CHUNK_SIZE = 1000;

	@Override
	public int processDailyStatistics(LocalDate targetDate) {
		return processDailyStatistics(targetDate, DEFAULT_CHUNK_SIZE);
	}

	public int processDailyStatistics(LocalDate targetDate, int chunkSize) {
		log.info("Processing hourly data to daily aggregation for date: {} with chunk size: {}", targetDate, chunkSize);

		// DB에서 해당 날짜의 모든 시간별 통계 조회
		List<UrlMetricsHourlyEntity> hourlyMetrics = urlMetricsWriterPort.findAllHourlyMetricsByDate(targetDate);
		if (hourlyMetrics.isEmpty()) {
			log.info("No hourly metrics found for date: {}", targetDate);
			return 0;
		}

		log.info("Found {} hourly metrics records for date: {}", hourlyMetrics.size(), targetDate);

		// 코드별로 그룹핑하여 집계
		Map<String, List<UrlMetricsHourlyEntity>> codeGroupedMetrics = new HashMap<>();
		for (UrlMetricsHourlyEntity hourly : hourlyMetrics) {
			codeGroupedMetrics.computeIfAbsent(hourly.getCode(), k -> new java.util.ArrayList<>()).add(hourly);
		}

		// 코드 청크 단위로 처리 (메모리 효율적으로 변경)
		List<String> codes = codeGroupedMetrics.keySet().stream().collect(Collectors.toList());
		int totalCodes = codes.size();
		int processedCodes = 0;
		int failedCodes = 0;

		log.info("Processing {} codes in chunks of {}", totalCodes, chunkSize);

		for (int i = 0; i < totalCodes; i += chunkSize) {
			int endIndex = Math.min(i + chunkSize, totalCodes);
			List<String> chunk = codes.subList(i, endIndex);

			log.info("Processing chunk {}-{} of {} codes", i + 1, endIndex, totalCodes);

			try {
				int chunkProcessed = self.processCodeChunk(chunk, targetDate);
				processedCodes += chunkProcessed;
				failedCodes += (chunk.size() - chunkProcessed);
			} catch (Exception e) {
				log.error("Failed to process chunk {}-{} on date: {}", i + 1, endIndex, targetDate, e);
				failedCodes += chunk.size();
			}
		}

		log.info("Successfully processed {}/{} codes for daily aggregation on date: {} (failed: {})",
			processedCodes, totalCodes, targetDate, failedCodes);
		return processedCodes;
	}

	@Transactional
	public int processCodeChunk(List<String> codesToProcess, LocalDate targetDate) {
		int processedInChunk = 0;

		for (String code : codesToProcess) {
			try {
				// 1) PV 합계 (DB hourly 테이블에서)
				long totalAccesses = urlMetricsWriterPort.sumHourlyAccesses(code, targetDate);

				// 2) UV 수 (Redis HLL PFCOUNT)
				long uniqueVisitors = redisStatisticsReaderPort.getDailyUniqueVisitorsCount(code, targetDate);

				// 3) 디바이스 PV (Redis 일별 해시)
				Map<String, Long> devicePv = Optional.ofNullable(
					redisStatisticsReaderPort.getDeviceStatistics(code, targetDate)
				).orElseGet(Map::of);
				String devicePvJson = objectMapper.writeValueAsString(devicePv);

				// 4) 디바이스 UV (Redis 각 디바이스별 HLL PFCOUNT)
				Map<String, Long> deviceUv = Optional.ofNullable(
					redisStatisticsReaderPort.getDailyDeviceUniqueVisitorsCount(code, targetDate)
				).orElseGet(Map::of);
				String deviceUvJson = objectMapper.writeValueAsString(deviceUv);

				// 5) UPSERT daily
				UrlMetricsDailyEntity daily = urlMetricsWriterPort.findDailyMetrics(code, targetDate)
					.orElseGet(() -> UrlMetricsDailyEntity.builder()
						.code(code)
						.day(targetDate)
						.accesses(0L)
						.uniqueVisitors(0L)
						.deviceAccessesJson("{}")
						.deviceUniqueVisitorsJson("{}")
						.build());

				daily.setAccesses(totalAccesses);
				daily.setUniqueVisitors(uniqueVisitors);
				daily.setDeviceAccessesJson(devicePvJson);
				daily.setDeviceUniqueVisitorsJson(deviceUvJson);

				urlMetricsWriterPort.saveDailyMetrics(daily);
				processedInChunk++;

				log.debug("Aggregated daily statistics for code: {} on date: {}, PV: {}, UV: {}",
					code, targetDate, totalAccesses, uniqueVisitors);

			} catch (Exception e) {
				log.error("Failed to aggregate daily statistics for code: {} on date: {}", code, targetDate, e);
				// 개별 코드 실패는 청크 전체를 롤백하지 않고 계속 진행
			}
		}

		return processedInChunk;
	}

	@Override
	public boolean processCodeDailyStatistics(String code, LocalDate targetDate) {
		try {
			// 단일 코드에 대해 청크 처리와 동일한 로직 사용
			return self.processCodeChunk(List.of(code), targetDate) > 0;
		} catch (Exception e) {
			log.error("Failed to process single code daily statistics: {} on date: {}", code, targetDate, e);
			return false;
		}
	}
}
