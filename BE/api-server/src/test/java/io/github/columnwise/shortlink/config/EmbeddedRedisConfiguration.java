package io.github.columnwise.shortlink.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class EmbeddedRedisConfiguration {

    private RedisServer redisServer;
    
    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServer(6370); // 다른 포트 사용
        redisServer.start();
    }
    
    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}