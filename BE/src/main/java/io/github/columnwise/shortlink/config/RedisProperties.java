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
    
    @Valid
    @NotNull
    private HitCounter hitCounter = new HitCounter();
    
    @Valid
    @NotNull
    private Lock lock = new Lock();
    
    @Data
    public static class Cache {
        @NotBlank(message = "Cache key prefix cannot be blank")
        private String keyPrefix = "shorturl:";
        
        @NotNull(message = "Cache default TTL cannot be null")
        private Duration defaultTtl = Duration.ofMinutes(30);
    }
    
    @Data
    public static class HitCounter {
        @NotBlank(message = "HitCounter key prefix cannot be blank")
        private String keyPrefix = "hitcount:";
    }
    
    @Data
    public static class Lock {
        @NotBlank(message = "Lock key prefix cannot be blank")
        private String keyPrefix = "lock:";
        
        @NotNull(message = "Lock default expiration cannot be null")
        private Duration defaultExpiration = Duration.ofSeconds(30);
    }
}