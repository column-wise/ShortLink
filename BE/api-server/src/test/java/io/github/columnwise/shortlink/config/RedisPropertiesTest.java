package io.github.columnwise.shortlink.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class RedisPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.afterPropertiesSet();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("기본값으로 생성된 RedisProperties는 유효함")
    void defaultRedisProperties_IsValid() {
        // Given
        RedisProperties properties = new RedisProperties();

        // When
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Cache keyPrefix가 null이면 검증 실패")
    void cache_NullKeyPrefix_ValidationFails() {
        // Given
        RedisProperties properties = new RedisProperties();
        properties.getCache().setKeyPrefix(null);

        // When
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Cache key prefix cannot be blank");
    }

    @Test
    @DisplayName("Cache keyPrefix가 빈 문자열이면 검증 실패")
    void cache_EmptyKeyPrefix_ValidationFails() {
        // Given
        RedisProperties properties = new RedisProperties();
        properties.getCache().setKeyPrefix("");

        // When
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Cache key prefix cannot be blank");
    }

    @Test
    @DisplayName("Cache defaultTtl이 null이면 검증 실패")
    void cache_NullDefaultTtl_ValidationFails() {
        // Given
        RedisProperties properties = new RedisProperties();
        properties.getCache().setDefaultTtl(null);

        // When
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Cache default TTL cannot be null");
    }


    @Test
    @DisplayName("HitCounter keyPrefix가 null이면 검증 실패")
    void hitCounter_NullKeyPrefix_ValidationFails() {
        // Given
        RedisProperties properties = new RedisProperties();
        properties.getHitCounter().setKeyPrefix(null);

        // When
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("HitCounter key prefix cannot be blank");
    }

    @Test
    @DisplayName("Lock keyPrefix가 빈 문자열이면 검증 실패")
    void lock_EmptyKeyPrefix_ValidationFails() {
        // Given
        RedisProperties properties = new RedisProperties();
        properties.getLock().setKeyPrefix("");

        // When
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Lock key prefix cannot be blank");
    }

    @Test
    @DisplayName("Lock defaultExpiration이 null이면 검증 실패")
    void lock_NullDefaultExpiration_ValidationFails() {
        // Given
        RedisProperties properties = new RedisProperties();
        properties.getLock().setDefaultExpiration(null);

        // When
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Lock default expiration cannot be null");
    }


    @Test
    @DisplayName("여러 검증 실패가 동시에 발생할 수 있음")
    void multipleValidationFailures() {
        // Given
        RedisProperties properties = new RedisProperties();
        properties.getCache().setKeyPrefix("");
        properties.getCache().setDefaultTtl(null);
        properties.getHitCounter().setKeyPrefix(null);

        // When
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).hasSize(3);
    }

    @Test
    @DisplayName("유효한 커스텀 설정값으로 검증 통과")
    void customValidSettings_PassValidation() {
        // Given
        RedisProperties properties = new RedisProperties();
        properties.getCache().setKeyPrefix("custom:cache:");
        properties.getCache().setDefaultTtl(Duration.ofHours(1));
        properties.getHitCounter().setKeyPrefix("custom:hits:");
        properties.getLock().setKeyPrefix("custom:lock:");
        properties.getLock().setDefaultExpiration(Duration.ofMinutes(5));

        // When
        Set<ConstraintViolation<RedisProperties>> violations = validator.validate(properties);

        // Then
        assertThat(violations).isEmpty();
    }
}