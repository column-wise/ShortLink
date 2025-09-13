package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.GetStatsUseCase;
import io.github.columnwise.shortlink.application.port.out.StatisticsRepository;
import io.github.columnwise.shortlink.domain.model.DailyStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetStatsService implements GetStatsUseCase {
    
    private final StatisticsRepository statisticsRepository;
    
    @Override
    public List<DailyStatistics> getDailyStatistics(String code, LocalDate startDate, LocalDate endDate) {
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
