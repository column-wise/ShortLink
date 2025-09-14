package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.GetStatsUseCase;
import io.github.columnwise.shortlink.application.port.out.StatisticsRepository;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.model.DailyStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetStatsService implements GetStatsUseCase {

    private final StatisticsRepository statisticsRepository;
    private final ShortUrlRepositoryPort shortUrlRepositoryPort;

    @Override
    public List<DailyStatistics> getDailyStatistics(String code, LocalDate startDate, LocalDate endDate) {
        // 먼저 코드가 존재하는지 확인
        shortUrlRepositoryPort.findByCode(code)
            .orElseThrow(() -> new UrlNotFoundException("URL not found for code: " + code));

        // 기본값 설정
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);  // 기본 30일
        }

        return statisticsRepository.getDailyStatistics(code, startDate, endDate);
    }
}
