package io.github.columnwise.shortlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;

@SpringBootApplication
@EnableBatchProcessing
public class BatchApplication {

    /**
     * Application entry point that boots the Spring Boot context configured for batch processing.
     *
     * <p>Invokes SpringApplication.run with this class as the primary source to start the application.</p>
     *
     * @param args command-line arguments passed to the application (forwarded to SpringApplication)
     */
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}