package io.github.columnwise.shortlink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * URL 단축 서비스 관련 설정 프로퍼티
 *
 * <p>application.properties 또는 application.yml에서
 * {@code app.shorturl} 접두사를 가진 설정값들을 바인딩합니다.</p>
 *
 * <p>설정 예시:</p>
 * <pre>
 * app.shorturl.max-retries=5
 * app.shorturl.default-expiration-days=365
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "app.shorturl")
public class ShortUrlProperties {

    /**
     * 코드 생성 시 최대 재시도 횟수
     */
    private int maxRetries = 5;

    /**
     * URL 기본 만료 기간 (일)
     */
    private long defaultExpirationDays = 365;
}