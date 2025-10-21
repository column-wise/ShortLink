package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.config.ShortUrlProperties;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 독립적인 트랜잭션에서 ShortUrl 저장을 담당하는 컴포넌트
 *
 * <p>Spring의 트랜잭션 프록시 메커니즘을 올바르게 활용하기 위해 별도 컴포넌트로 분리되었습니다.
 * {@code @Transactional(propagation = Propagation.REQUIRES_NEW)}는 동일 클래스 내부 호출에서는
 * 프록시를 우회하여 작동하지 않기 때문에 외부 빈으로 분리가 필요합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>독립적인 새 트랜잭션에서 ShortUrl 저장</li>
 *   <li>동시성 대응을 위한 중복 확인</li>
 *   <li>rollback-only 상태 방지</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
class ShortUrlTxSaver {

    private final ShortUrlRepositoryPort shortUrlRepository;
    private final ShortUrlProperties properties;
    private final Clock clock;

    /**
     * 새로운 독립적인 트랜잭션에서 ShortUrl을 저장합니다.
     *
     * <p>이 메서드는 호출자의 트랜잭션과 완전히 독립적으로 실행되어
     * 저장 실패 시에도 호출자의 트랜잭션에 영향을 주지 않습니다.</p>
     *
     * @param longUrl 원본 URL
     * @param code 생성된 단축 코드
     * @return 저장된 ShortUrl 객체
     * @throws DataIntegrityViolationException 데이터베이스 제약 위반 시
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    ShortUrl saveWithNewTx(String longUrl, String code) {
        // 실제 저장 전 한번 더 중복 확인 (동시성 대응)
        if (shortUrlRepository.findByCode(code).isPresent()) {
            throw new DataIntegrityViolationException("Code already exists: " + code);
        }

        Instant now = clock.instant();
        ShortUrl shortUrl = ShortUrl.builder()
                .code(code)
                .longUrl(longUrl)
                .createdAt(now)
                .expiresAt(now.plus(properties.getDefaultExpirationDays(), ChronoUnit.DAYS))
                .isActive(true)
                .build();

        return shortUrlRepository.save(shortUrl);
    }
}
