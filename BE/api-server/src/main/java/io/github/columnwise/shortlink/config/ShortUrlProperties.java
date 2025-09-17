package io.github.columnwise.shortlink.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
 *
 * <p>모든 설정값은 Bean Validation을 통해 검증됩니다.</p>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.shorturl")
public class ShortUrlProperties {

    /**
     * 코드 생성 시 최대 재시도 횟수
     *
     * <p>1 이상의 값이어야 합니다. 너무 높은 값은 성능에 영향을 줄 수 있으므로
     * 적절한 범위(1-10)에서 설정하는 것을 권장합니다.</p>
     */
    @Min(value = 1, message = "maxRetries must be at least 1")
    private int maxRetries = 5;

    /**
     * URL 기본 만료 기간 (일)
     *
     * <p>양수 값이어야 합니다. 일반적으로 30일에서 365일 사이의 값을 권장합니다.</p>
     */
    @Positive(message = "defaultExpirationDays must be positive")
    private long defaultExpirationDays = 365;

    /**
     * Redis 방문 통계 TTL (일)
     *
     * <p>Redis에 저장되는 방문 통계 데이터의 보관 기간입니다.
     * 양수 값이어야 하며, batch-server에서 1시간 단위로 영속화하기 때문에 padding을 주어 2일을 기본값으로 하겠습니다.</p>
     */
    @Positive(message = "visitStatisticsTtlDays must be positive")
    private long visitStatisticsTtlDays = 2;
}