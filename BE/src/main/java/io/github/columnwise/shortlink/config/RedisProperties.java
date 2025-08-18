package io.github.columnwise.shortlink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {
    
    private Cache cache = new Cache();
    private HitCounter hitCounter = new HitCounter();
    private Lock lock = new Lock();
    
    @Data
    public static class Cache {
        private String keyPrefix = "shorturl:";
        private Duration defaultTtl = Duration.ofMinutes(30);
    }
    
    @Data
    public static class HitCounter {
        private String keyPrefix = "hitcount:";
    }
    
    @Data
    public static class Lock {
        private String keyPrefix = "lock:";
        private Duration defaultExpiration = Duration.ofSeconds(30);
    }
}