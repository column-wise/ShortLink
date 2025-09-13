package io.github.columnwise.shortlink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WebConfig {
    
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
