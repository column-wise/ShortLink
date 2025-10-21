package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.config.ShortUrlProperties;
import io.github.columnwise.shortlink.util.HashUtils;
import io.github.columnwise.shortlink.util.UriSchemeValidator;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.exception.InvalidUriSchemeException;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * URL 해석 서비스 구현체
 *
 * <p>단축 코드를 통해 원본 URL을 찾고 방문 기록 전송(향후 Kafka)을 수행하는 서비스입니다.</p>
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
    private final ShortUrlProperties properties;
    private final Clock clock;
    // TODO(kafka): 방문 이벤트를 Kafka 토픽으로 발행하도록 전환 예정
    
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

        // URI 스킴 검증 - 통계 기록 전에 수행
        if (!UriSchemeValidator.isValidScheme(shortUrl.longUrl(), properties.getAllowedSchemes())) {
            throw new InvalidUriSchemeException("유효하지 않은 URL 스킴입니다. 허용되는 스킴: " + properties.getAllowedSchemes());
        }

        // 검증 통과한 경우 방문 이벤트를 비동기로 전송(추후 Kafka 연동)
        // publishVisitEvent(code, ip, uaFamily, deviceType);

        return shortUrl.longUrl();
    }
    
    /**
     * 방문 이벤트를 비동기 스트림으로 전송합니다(Kafka 연동 예정).
     */
    private void publishVisitEvent(String code, String ip, String uaFamily, String deviceType) {
        // placeholder: Kafka 프로듀서 연동 예정
    }
}
