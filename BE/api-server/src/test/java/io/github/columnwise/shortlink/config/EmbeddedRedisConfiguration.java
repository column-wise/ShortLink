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
        int port = findAvailablePort();
        redisServer = new RedisServer(port);
        redisServer.start();
        System.setProperty("spring.redis.port", String.valueOf(port));
    }
    
    private int findAvailablePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Failed to find available port", e);
        }
    }
    
    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}