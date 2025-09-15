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

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Clock;
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
    private final ShortUrlTxSaver txSaver;
    
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
                ShortUrl savedUrl = txSaver.saveWithNewTx(longUrl, code);
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
     * 데이터베이스 제약 위반이 고유 코드 제약 위반인지 확인합니다.
     *
     * <p>SQLState 코드와 표준 예외 타입을 우선적으로 확인하여 보다 정확한 판별을 수행합니다.
     * 문자열 매칭은 보조적으로만 사용하여 벤더별 메시지 차이에 대응합니다.</p>
     *
     * @param e DataIntegrityViolationException
     * @return 고유 코드 제약 위반 여부
     */
    private boolean isUniqueCodeConstraintViolation(DataIntegrityViolationException e) {
        // 1. SQLState 및 표준 예외 타입으로 우선 확인
        Throwable rootCause = e.getRootCause();
        if (rootCause instanceof SQLException sqlException) {
            String sqlState = sqlException.getSQLState();
            // PostgreSQL, H2: 23505 (unique_violation)
            if ("23505".equals(sqlState)) {
                return true;
            }
            // MySQL: 에러 코드 1062 (ER_DUP_ENTRY)
            if (sqlException.getErrorCode() == 1062) {
                return true;
            }
            // 표준 SQL 무결성 제약 위반 예외
            if (sqlException instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
        }

        // 2. 메시지 기반 폴백 확인 (벤더별 차이 대응)
        String message = Optional.ofNullable(e.getMessage())
                .map(String::toLowerCase)
                .orElse("");
        return message.contains("unique") ||
               message.contains("duplicate") ||
               message.contains("duplicate key") ||
               message.contains("code already exists");
    }

    /**
     * 로깅용 URL 마스킹 처리
     *
     * <p>보안상 민감할 수 있는 URL 정보를 SHA-256 해시로 마스킹하여 로그에 기록합니다.
     * 해시를 통해 개인정보 노출을 방지하면서도 디버깅 시 동일한 URL을 식별할 수 있습니다.</p>
     *
     * @param url 원본 URL
     * @return 마스킹된 URL (hash=...)
     */
    private String maskUrl(String url) {
        if (url == null) {
            return "n/a";
        }

        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            var b64 = java.util.Base64.getEncoder().encodeToString(
                md.digest(url.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return "hash=" + b64.substring(0, 16);
        } catch (Exception ignore) {
            return "hash=n/a";
        }
    }
}
