package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.config.ShortUrlProperties;
import io.github.columnwise.shortlink.util.HashUtils;
import io.github.columnwise.shortlink.util.UriSchemeValidator;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.exception.InvalidUriSchemeException;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.application.port.out.StreamPort;
import io.github.columnwise.shortlink.domain.event.VisitEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * URL 해석 서비스 구현체
 *
 * <p>단축 코드를 통해 원본 URL을 찾고 방문 이벤트를 메시지 스트림으로 발생하는 서비스입니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>단축 코드로 원본 URL 조회</li>
 *   <li>방문 이벤트 스트림 발생</li>
 *   <li>비동기 이벤트 처리를 통한 실시간 통계 지원</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ResolveUrlService implements ResolveUrlUseCase {

    private final ShortUrlRepositoryPort shortUrlRepository;
    private final ShortUrlProperties properties;
    private final Clock clock;
    private final StreamPort streamPort;

    /**
     * 단축 코드를 통해 원본 URL을 조회하고 방문을 기록합니다.
     *
     * <p>주어진 단축 코드에 해당하는 원본 URL을 데이터베이스에서 조회하고,
     * 방문 이벤트를 스트림으로 발행합니다.</p>
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

        // 검증 통과한 경우 방문 이벤트를 비동기로 전송
        publishVisitEvent(code, ip, uaFamily, deviceType, null);

        return shortUrl.longUrl();
    }
    
    /**
     * 방문 이벤트를 비동기 스트림으로 전송합니다.
     */
    private void publishVisitEvent(String code, String ip, String uaFamily, String deviceType, String referer) {
        String ua = (uaFamily == null || uaFamily.isBlank()) ? "unknown" : uaFamily;
        String device = (deviceType == null || deviceType.isBlank()) ? "unknown" : deviceType;
        String visitorHash = HashUtils.sha256((ip == null ? "" : ip) + "|" + ua);

        VisitEvent event = new VisitEvent(
                java.util.UUID.randomUUID().toString(),
                code,
                visitorHash,
                ua,
                device,
                referer,
                clock.instant()
        );
        streamPort.publish(event);
    }
}
