package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.config.ShortUrlProperties;
import io.github.columnwise.shortlink.domain.service.RedisKeyManager;
import org.springframework.data.redis.core.RedisTemplate;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * URL 해석 서비스 구현체
 *
 * <p>단축 코드를 통해 원본 URL을 찾고 방문 기록을 저장하는 서비스입니다.
 * Redis를 활용하여 실시간 방문 통계를 기록합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>단축 코드로 원본 URL 조회</li>
 *   <li>방문 시간 기반 실시간 통계 기록</li>
 *   <li>타임스탬프 기반 방문 로그 저장</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ResolveUrlService implements ResolveUrlUseCase {

    private final ShortUrlRepositoryPort shortUrlRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ShortUrlProperties properties;
    private final Clock clock;
    
    /**
     * 단축 코드를 통해 원본 URL을 조회하고 방문을 기록합니다.
     *
     * <p>주어진 단축 코드에 해당하는 원본 URL을 데이터베이스에서 조회하고,
     * Redis에 방문 기록을 타임스탬프와 함께 저장합니다.</p>
     *
     * @param code 단축 코드
     * @return 원본 URL
     * @throws UrlNotFoundException 해당 코드에 대한 URL이 존재하지 않는 경우
     */
    @Override
    public String resolveUrl(String code) {
        ShortUrl shortUrl = shortUrlRepository.findByCode(code)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for code: " + code));
        
        // Redis에 타임스탬프 기반 방문 기록 저장
        recordVisit(code);
        
        return shortUrl.longUrl();
    }
    
    /**
     * 방문 기록을 Redis에 저장합니다.
     *
     * <p>현재 시간을 기반으로 일별 카운터를 증가시키고 TTL을 설정하여 자동 정리합니다.
     * 개별 타임스탬프 키 대신 일별 집계된 카운터를 사용하여 성능을 최적화합니다.</p>
     *
     * @param code 방문된 단축 코드
     */
    private void recordVisit(String code) {
        LocalDateTime now = LocalDateTime.now(clock);
        String dayBucket = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String counterKey = "url:access:count:" + code + ":" + dayBucket;

        // 일별 카운터 증가 및 TTL 설정 (설정 가능한 보관 기간)
        redisTemplate.opsForValue().increment(counterKey);
        redisTemplate.expire(counterKey, java.time.Duration.ofDays(properties.getVisitStatisticsTtlDays()));
    }
}
