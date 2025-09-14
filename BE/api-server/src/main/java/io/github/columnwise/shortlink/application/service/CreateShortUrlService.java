package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.in.CreateShortUrlUseCase;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.config.ShortUrlProperties;
import io.github.columnwise.shortlink.domain.exception.CodeCollisionException;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.service.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * URL 단축 서비스 구현체
 *
 * <p>주어진 긴 URL을 짧은 코드로 변환하는 서비스입니다.
 * 코드 충돌 방지를 위한 재시도 로직과 트랜잭션 관리를 제공합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>기존 URL이 있는 경우 기존 코드 반환</li>
 *   <li>코드 생성 시 충돌 방지를 위한 재시도 로직</li>
 *   <li>데이터베이스 제약 위반과 예상치 못한 오류 구분 처리</li>
 *   <li>상세한 로깅을 통한 디버깅 지원</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateShortUrlService implements CreateShortUrlUseCase {
    
    private final ShortUrlRepositoryPort shortUrlRepository;
    private final CodeGenerator codeGenerator;
    private final ShortUrlProperties properties;
    private final Clock clock;
    
    /**
     * 긴 URL을 단축 코드로 변환합니다.
     *
     * <p>기존에 동일한 URL이 존재하는 경우 기존 코드를 반환하고,
     * 그렇지 않은 경우 새로운 단축 코드를 생성합니다.</p>
     *
     * <p>코드 생성 과정에서 충돌이 발생하면 최대 재시도 횟수까지
     * 다른 코드로 재시도를 수행합니다.</p>
     *
     * @param longUrl 단축할 긴 URL
     * @return 생성된 ShortUrl 객체
     * @throws CodeCollisionException 최대 재시도 후에도 고유한 코드를 생성하지 못한 경우
     * @throws RuntimeException 예상치 못한 오류가 발생한 경우
     */
    @Override
    @Transactional
    public ShortUrl createShortUrl(String longUrl) {
        log.debug("Creating short URL for: {}", longUrl);

        // 기존 URL이 있으면 반환
        Optional<ShortUrl> existing = shortUrlRepository.findByLongUrl(longUrl);
        if (existing.isPresent()) {
            log.debug("Found existing short URL: {} for {}", existing.get().code(), longUrl);
            return existing.get();
        }
        
        // 코드 충돌 방지를 위한 재시도 로직
        int maxRetries = properties.getMaxRetries();
        log.debug("Starting code generation with max {} retries", maxRetries);

        for (int i = 0; i < maxRetries; i++) {
            String code = codeGenerator.generate(longUrl + "_" + i); // salt 추가
            log.debug("Generated code: {} (attempt {}/{})", code, i + 1, maxRetries);

            // 코드 중복 확인
            if (shortUrlRepository.findByCode(code).isEmpty()) {
                Instant now = clock.instant();
                ShortUrl shortUrl = ShortUrl.builder()
                        .code(code)
                        .longUrl(longUrl)
                        .createdAt(now)
                        .expiresAt(now.plus(properties.getDefaultExpirationDays(), ChronoUnit.DAYS))
                        .build();
                        
                try {
                    ShortUrl savedUrl = shortUrlRepository.save(shortUrl);
                    log.info("Successfully created short URL: {} for {}", code, longUrl);
                    return savedUrl;
                } catch (DataIntegrityViolationException e) {
                    // 데이터베이스 제약 위반 (중복 코드) - 재시도
                    log.warn("Database constraint violation for code: {} (attempt {}/{})", code, i + 1, maxRetries);
                    if (i == maxRetries - 1) {
                        log.error("Failed to generate unique code after {} attempts for URL: {}", maxRetries, longUrl);
                        throw new CodeCollisionException("Failed to generate unique code after " + maxRetries + " attempts due to database constraint violation", e);
                    }
                    // 다음 반복에서 재시도
                } catch (Exception e) {
                    // 예상치 못한 오류는 즉시 실패
                    log.error("Unexpected error while saving short URL for: {}", longUrl, e);
                    throw new RuntimeException("Unexpected error occurred while saving short URL", e);
                }
            } else {
                log.debug("Code collision detected for: {}, retrying...", code);
            }
        }
        
        log.error("Exhausted all {} retry attempts for URL: {}", maxRetries, longUrl);
        throw new CodeCollisionException("Failed to generate unique code after exhausting all retry attempts");
    }
}
