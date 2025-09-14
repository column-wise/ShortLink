package io.github.columnwise.shortlink.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 웹 애플리케이션 설정
 *
 * <p>시간 관련 Bean과 URL 단축 서비스 설정을 제공합니다.</p>
 */
@Configuration
@EnableConfigurationProperties(ShortUrlProperties.class)
public class WebConfig {

    /**
     * 시스템 기본 타임존을 사용하는 Clock Bean
     *
     * <p>테스트 가능한 시간 처리를 위해 Clock Bean을 제공합니다.
     * 테스트 시에는 고정된 Clock으로 교체하여 일관된 시간 테스트를 수행할 수 있습니다.</p>
     *
     * @return 시스템 기본 Clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
