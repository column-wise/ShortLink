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
import org.springframework.transaction.annotation.Propagation;

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
    @Transactional(readOnly = true)
    public ShortUrl createShortUrl(String longUrl) {
        String maskedUrl = maskUrl(longUrl);
        log.debug("Creating short URL for: {}", maskedUrl);

        // 기존 URL이 있으면 반환
        Optional<ShortUrl> existing = shortUrlRepository.findByLongUrl(longUrl);
        if (existing.isPresent()) {
            log.debug("Found existing short URL: {} for {}", existing.get().code(), maskedUrl);
            return existing.get();
        }

        // 코드 충돌 방지를 위한 재시도 로직
        int maxRetries = properties.getMaxRetries();
        log.debug("Starting code generation with max {} retries for {}", maxRetries, maskedUrl);

        for (int i = 0; i < maxRetries; i++) {
            String code = codeGenerator.generate(longUrl + "_" + i); // salt 추가
            log.debug("Generated code: {} (attempt {}/{}) for {}", code, i + 1, maxRetries, maskedUrl);

            try {
                ShortUrl savedUrl = saveWithNewTransaction(longUrl, code, i + 1, maxRetries);
                log.info("Successfully created short URL: {} for {} after {} attempts", code, maskedUrl, i + 1);
                return savedUrl;
            } catch (DataIntegrityViolationException e) {
                if (isUniqueCodeConstraintViolation(e)) {
                    log.warn("Unique code constraint violation for code: {} (attempt {}/{}) for {}",
                            code, i + 1, maxRetries, maskedUrl);
                    if (i == maxRetries - 1) {
                        log.error("Failed to generate unique code after {} attempts for {}", maxRetries, maskedUrl);
                        throw new CodeCollisionException("Failed to generate unique code after " + maxRetries + " attempts due to unique constraint violation", e);
                    }
                    // 다음 반복에서 재시도
                } else {
                    // 다른 제약 위반은 재시도하지 않음
                    log.error("Non-recoverable constraint violation for code: {} for {}", code, maskedUrl, e);
                    throw new RuntimeException("Database constraint violation that cannot be resolved by retry", e);
                }
            } catch (Exception e) {
                // 예상치 못한 오류는 즉시 실패
                log.error("Unexpected error while saving short URL for: {}", maskedUrl, e);
                throw new RuntimeException("Unexpected error occurred while saving short URL", e);
            }
        }

        log.error("Exhausted all {} retry attempts for {}", maxRetries, maskedUrl);
        throw new CodeCollisionException("Failed to generate unique code after exhausting all retry attempts");
    }

    /**
     * 새로운 트랜잭션에서 ShortUrl을 저장합니다.
     *
     * <p>각 저장 시도를 독립적인 트랜잭션으로 실행하여 rollback-only 상태를 방지합니다.</p>
     *
     * @param longUrl 원본 URL
     * @param code 생성된 단축 코드
     * @param attempt 현재 시도 번호
     * @param maxRetries 최대 재시도 횟수
     * @return 저장된 ShortUrl 객체
     * @throws DataIntegrityViolationException 데이터베이스 제약 위반 시
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ShortUrl saveWithNewTransaction(String longUrl, String code, int attempt, int maxRetries) {
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
                .build();

        return shortUrlRepository.save(shortUrl);
    }

    /**
     * 데이터베이스 제약 위반이 고유 코드 제약 위반인지 확인합니다.
     *
     * @param e DataIntegrityViolationException
     * @return 고유 코드 제약 위반 여부
     */
    private boolean isUniqueCodeConstraintViolation(DataIntegrityViolationException e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        // 일반적인 unique constraint violation 패턴 확인
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("unique") ||
               lowerMessage.contains("duplicate") ||
               lowerMessage.contains("code already exists");
    }

    /**
     * 로깅용 URL 마스킹 처리
     *
     * <p>보안상 민감할 수 있는 URL 정보를 마스킹하여 로그에 기록합니다.</p>
     *
     * @param url 원본 URL
     * @return 마스킹된 URL
     */
    private String maskUrl(String url) {
        if (url == null || url.length() <= 10) {
            return url;
        }

        // 프로토콜과 도메인은 유지하고 경로는 마스킹
        int protocolEnd = url.indexOf("://");
        if (protocolEnd == -1) {
            return url.substring(0, 10) + "***";
        }

        int pathStart = url.indexOf("/", protocolEnd + 3);
        if (pathStart == -1) {
            return url; // 경로가 없으면 그대로 반환
        }

        String baseUrl = url.substring(0, pathStart);
        return baseUrl + "/***";
    }
}
