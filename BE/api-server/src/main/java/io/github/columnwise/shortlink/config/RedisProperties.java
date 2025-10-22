package io.github.columnwise.shortlink.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.redis")
@Validated
public class RedisProperties {
    
    @Valid
    @NotNull
    private Cache cache = new Cache();
    
    @Data
    public static class Cache {
        @NotBlank(message = "Cache key prefix cannot be blank")
        private String keyPrefix = "shorturl:";
        
        @NotNull(message = "Cache default TTL cannot be null")
        private Duration defaultTtl = Duration.ofMinutes(30);
    }
}