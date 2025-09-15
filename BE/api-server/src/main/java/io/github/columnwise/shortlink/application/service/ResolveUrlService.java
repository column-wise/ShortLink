package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.config.ShortUrlProperties;
import io.github.columnwise.shortlink.domain.service.RedisKeyManager;
import io.github.columnwise.shortlink.util.HashUtils;
import org.springframework.data.redis.core.RedisTemplate;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
     * Redis에 방문 기록을 저장합니다.</p>
     *
     * @param code 단축 코드
     * @return 원본 URL
     * @throws UrlNotFoundException 해당 코드에 대한 URL이 존재하지 않는 경우
     */
    @Override
    public String resolveUrl(String code, String ip, String uaFamily, String deviceType) {
        ShortUrl shortUrl = shortUrlRepository.findByCode(code)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for code: " + code));
        
        // Redis에 타임스탬프 기반 방문 기록 저장
        recordVisit(code, ip, uaFamily, deviceType);
        
        return shortUrl.longUrl();
    }
    
    /**
     * 방문 기록을 Redis에 저장합니다.
     *
     * <p>현재 시간을 기반으로 키를 생성하여 방문을 기록합니다.
     * 코드 별 방문 정보는 key hour count 형태로 Hash에 저장됩니다.</p>
     *
     * @param code 방문된 단축 코드
     */
    private void recordVisit(String code, String ip, String uaFamily, String deviceType) {
        LocalDate today = LocalDate.now(clock);
        int hour = LocalDateTime.now(clock).getHour();

        String visitorHash = HashUtils.sha256(ip + "|" + uaFamily);

        String hourlyKey  = RedisKeyManager.getHourlyAccessKey(code, today);
        String uniqueKey  = RedisKeyManager.getDailyUniqueKey(code, today);
        String uaKey      = RedisKeyManager.getDailyUaKey(code, today);
        String deviceKey  = RedisKeyManager.getDailyDeviceKey(code, today);

        // 시간대별
        redisTemplate.opsForHash().increment(hourlyKey, String.format("%02d", hour), 1);

        // 고유 방문자
        redisTemplate.opsForHyperLogLog().add(uniqueKey, visitorHash);

        // 브라우저
        redisTemplate.opsForHash().increment(uaKey, uaFamily, 1);

        // 디바이스
        redisTemplate.opsForHash().increment(deviceKey, deviceType, 1);

        // TTL은 키 최초 생성 시만
        if (Boolean.FALSE.equals(redisTemplate.hasKey(hourlyKey))) {
            redisTemplate.expire(hourlyKey, Duration.ofDays(2));
        }
        if (Boolean.FALSE.equals(redisTemplate.hasKey(uniqueKey))) {
            redisTemplate.expire(uniqueKey, Duration.ofDays(2));
        }
        if (Boolean.FALSE.equals(redisTemplate.hasKey(uaKey))) {
            redisTemplate.expire(uaKey, Duration.ofDays(2));
        }
        if (Boolean.FALSE.equals(redisTemplate.hasKey(deviceKey))) {
            redisTemplate.expire(deviceKey, Duration.ofDays(2));
        }
    }
}
