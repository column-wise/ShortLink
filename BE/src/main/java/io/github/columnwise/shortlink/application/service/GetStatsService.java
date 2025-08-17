package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.GetStatsUseCase;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.domain.model.UrlAccessLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetStatsService implements GetStatsUseCase {
    
    private final ShortUrlRepositoryPort shortUrlRepository;
    
    @Override
    public List<UrlAccessLog> getAccessLogs(String code) {
        return shortUrlRepository.findAccessLogsByCode(code);
    }
}
