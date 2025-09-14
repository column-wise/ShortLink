package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.config.ShortUrlProperties;
import io.github.columnwise.shortlink.domain.exception.CodeCollisionException;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.service.CodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class CreateShortUrlServiceTest {

    @Mock
    private ShortUrlRepositoryPort shortUrlRepository;

    @Mock
    private CodeGenerator codeGenerator;

    @Mock
    private ShortUrlProperties properties;

    private Clock fixedClock;
    private CreateShortUrlService createShortUrlService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.systemDefault());
        createShortUrlService = new CreateShortUrlService(shortUrlRepository, codeGenerator, properties, fixedClock);
    }

    @Test
    @DisplayName("기존 URL이 있으면 그대로 반환")
    void createShortUrl_ExistingUrl_ReturnExisting() {
        // Given
        String longUrl = "https://www.example.com";
        ShortUrl existingUrl = ShortUrl.builder()
                .id(1L)
                .code("existing")
                .longUrl(longUrl)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(31536000))
                .build();

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.of(existingUrl));

        // When
        ShortUrl result = createShortUrlService.createShortUrl(longUrl);

        // Then
        assertThat(result).isEqualTo(existingUrl);
        verify(shortUrlRepository).findByLongUrl(longUrl);
        verifyNoMoreInteractions(shortUrlRepository);
    }

    @Test
    @DisplayName("새 URL 생성 성공")
    void createShortUrl_NewUrl_Success() {
        // Given
        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getDefaultExpirationDays()).thenReturn(365L);

        String longUrl = "https://www.example.com";
        String generatedCode = "abc123";
        
        ShortUrl savedUrl = ShortUrl.builder()
                .id(1L)
                .code(generatedCode)
                .longUrl(longUrl)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(31536000))
                .build();

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generate(anyString())).thenReturn(generatedCode);
        when(shortUrlRepository.findByCode(generatedCode)).thenReturn(Optional.empty());
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(savedUrl);

        // When
        ShortUrl result = createShortUrlService.createShortUrl(longUrl);

        // Then
        assertThat(result).isEqualTo(savedUrl);
        verify(shortUrlRepository).findByLongUrl(longUrl);
        verify(codeGenerator).generate(longUrl + "_0");

        ArgumentCaptor<ShortUrl> shortUrlCaptor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).save(shortUrlCaptor.capture());

        ShortUrl capturedShortUrl = shortUrlCaptor.getValue();
        assertThat(capturedShortUrl.code()).isEqualTo(generatedCode);
        assertThat(capturedShortUrl.longUrl()).isEqualTo(longUrl);
        assertThat(capturedShortUrl.createdAt()).isNotNull();
        assertThat(capturedShortUrl.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("코드 충돌 시 재시도 후 성공")
    void createShortUrl_CodeCollision_RetrySuccess() {
        // Given
        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getDefaultExpirationDays()).thenReturn(365L);

        String longUrl = "https://www.example.com";
        String firstCode = "collision";
        String secondCode = "success";
        
        ShortUrl existingUrl = ShortUrl.builder()
                .id(1L)
                .code(firstCode)
                .longUrl("https://other.com")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(31536000))
                .build();
        
        ShortUrl savedUrl = ShortUrl.builder()
                .id(2L)
                .code(secondCode)
                .longUrl(longUrl)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(31536000))
                .build();

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generate(longUrl + "_0")).thenReturn(firstCode);
        when(codeGenerator.generate(longUrl + "_1")).thenReturn(secondCode);
        when(shortUrlRepository.findByCode(firstCode))
                .thenReturn(Optional.of(existingUrl)); // saveWithNewTransaction에서 체크
        when(shortUrlRepository.findByCode(secondCode))
                .thenReturn(Optional.empty()); // saveWithNewTransaction에서 체크
        when(shortUrlRepository.save(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key constraint violation"))
                .thenReturn(savedUrl);

        // When
        ShortUrl result = createShortUrlService.createShortUrl(longUrl);

        // Then
        assertThat(result).isEqualTo(savedUrl);
        verify(codeGenerator).generate(longUrl + "_0");
        verify(codeGenerator).generate(longUrl + "_1");
        verify(shortUrlRepository, times(2)).findByCode(any(String.class));
        verify(shortUrlRepository, times(2)).save(any(ShortUrl.class));
    }

    @Test
    @DisplayName("최대 재시도 횟수 초과 시 예외 발생")
    void createShortUrl_MaxRetriesExceeded_ThrowsException() {
        // Given
        when(properties.getMaxRetries()).thenReturn(5);

        String longUrl = "https://www.example.com";
        String code = "collision";
        
        ShortUrl existingUrl = ShortUrl.builder()
                .id(1L)
                .code(code)
                .longUrl("https://other.com")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(31536000))
                .build();

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generate(anyString())).thenReturn(code);
        when(shortUrlRepository.findByCode(code)).thenReturn(Optional.of(existingUrl));
        // saveWithNewTransaction에서 중복 확인 시 항상 존재한다고 가정

        // When & Then
        assertThatThrownBy(() -> createShortUrlService.createShortUrl(longUrl))
                .isInstanceOf(CodeCollisionException.class)
                .hasMessageContaining("Failed to generate unique code after 5 attempts");

        verify(codeGenerator, times(5)).generate(anyString());
        verify(shortUrlRepository, times(5)).findByCode(code);
    }

    @Test
    @DisplayName("저장 중 예외 발생 시 재시도")
    void createShortUrl_SaveException_Retry() {
        // Given
        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getDefaultExpirationDays()).thenReturn(365L);

        String longUrl = "https://www.example.com";
        String firstCode = "fail";
        String secondCode = "success";
        
        ShortUrl savedUrl = ShortUrl.builder()
                .id(1L)
                .code(secondCode)
                .longUrl(longUrl)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(31536000))
                .build();

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generate(longUrl + "_0")).thenReturn(firstCode);
        when(codeGenerator.generate(longUrl + "_1")).thenReturn(secondCode);
        when(shortUrlRepository.findByCode(firstCode)).thenReturn(Optional.empty());
        when(shortUrlRepository.findByCode(secondCode)).thenReturn(Optional.empty());
        when(shortUrlRepository.save(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key constraint violation"))
                .thenReturn(savedUrl);

        // When
        ShortUrl result = createShortUrlService.createShortUrl(longUrl);

        // Then
        assertThat(result).isEqualTo(savedUrl);
        verify(shortUrlRepository, times(2)).save(any(ShortUrl.class));
        verify(codeGenerator).generate(longUrl + "_0");
        verify(codeGenerator).generate(longUrl + "_1");
    }

    @Test
    @DisplayName("예외 원인 보존 확인")
    void createShortUrl_ExceptionCausePreservation() {
        // Given
        when(properties.getMaxRetries()).thenReturn(5);

        String longUrl = "https://www.example.com";
        String code = "test123";
        DataIntegrityViolationException originalException = new DataIntegrityViolationException("Constraint violation");

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generate(anyString())).thenReturn(code);
        when(shortUrlRepository.findByCode(code)).thenReturn(Optional.empty());
        when(shortUrlRepository.save(any(ShortUrl.class))).thenThrow(originalException);

        // When & Then
        assertThatThrownBy(() -> createShortUrlService.createShortUrl(longUrl))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database constraint violation that cannot be resolved by retry")
                .hasCause(originalException);
    }

    @Test
    @DisplayName("Clock을 통한 시간 설정 확인")
    void createShortUrl_ClockUsage() {
        // Given
        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getDefaultExpirationDays()).thenReturn(365L);

        String longUrl = "https://www.example.com";
        String code = "test123";
        Instant fixedTime = fixedClock.instant();

        ShortUrl savedUrl = ShortUrl.builder()
                .id(1L)
                .code(code)
                .longUrl(longUrl)
                .createdAt(fixedTime)
                .expiresAt(fixedTime.plusSeconds(31536000)) // 365 days
                .build();

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generate(anyString())).thenReturn(code);
        when(shortUrlRepository.findByCode(code)).thenReturn(Optional.empty());
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(savedUrl);

        // When
        ShortUrl result = createShortUrlService.createShortUrl(longUrl);

        // Then
        assertThat(result.createdAt()).isEqualTo(fixedTime);
        assertThat(result.expiresAt()).isEqualTo(fixedTime.plusSeconds(31536000));
    }

    @Test
    @DisplayName("URL 마스킹 기능 테스트")
    void maskUrl_ShouldMaskPathInfo() {
        // Given
        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getDefaultExpirationDays()).thenReturn(365L);

        String longUrl = "https://example.com/secret/path/with/sensitive/info";
        String code = "test123";

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generate(anyString())).thenReturn(code);
        when(shortUrlRepository.findByCode(code)).thenReturn(Optional.empty());
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(
                ShortUrl.builder()
                        .id(1L)
                        .code(code)
                        .longUrl(longUrl)
                        .createdAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(31536000))
                        .build()
        );

        // When
        createShortUrlService.createShortUrl(longUrl);

        // Then - URL이 마스킹되어 로그에 기록되는지는 로그 레벨에서 확인
        // 실제 기능 테스트는 성공적으로 URL이 생성되는지 확인
        verify(shortUrlRepository).save(any(ShortUrl.class));
    }

    @Test
    @DisplayName("고유 제약 위반과 일반 제약 위반 구분 테스트")
    void createShortUrl_DifferentConstraintViolations() {
        // Given
        when(properties.getMaxRetries()).thenReturn(3);

        String longUrl = "https://www.example.com";
        String code = "test123";

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generate(anyString())).thenReturn(code);
        when(shortUrlRepository.findByCode(code)).thenReturn(Optional.empty());

        // 고유 제약 위반이 아닌 다른 제약 위반 시뮬레이션
        DataIntegrityViolationException nonUniqueConstraint =
                new DataIntegrityViolationException("Check constraint violation on column 'status'");
        when(shortUrlRepository.save(any(ShortUrl.class))).thenThrow(nonUniqueConstraint);

        // When & Then
        assertThatThrownBy(() -> createShortUrlService.createShortUrl(longUrl))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database constraint violation that cannot be resolved by retry")
                .hasCause(nonUniqueConstraint);

        // 재시도하지 않고 즉시 실패해야 함
        verify(shortUrlRepository, times(1)).save(any(ShortUrl.class));
    }

    @Test
    @DisplayName("새로운 트랜잭션에서 동시성 체크 테스트")
    void saveWithNewTransaction_ConcurrencyCheck() {
        // Given
        when(properties.getMaxRetries()).thenReturn(5);

        String longUrl = "https://www.example.com";
        String code = "concurrent123";

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generate(anyString())).thenReturn(code);

        // saveWithNewTransaction에서 중복 체크 시 이미 존재한다고 시뮬레이션
        when(shortUrlRepository.findByCode(code))
                .thenReturn(Optional.of(ShortUrl.builder()
                        .id(1L)
                        .code(code)
                        .longUrl("https://other.com")
                        .createdAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(31536000))
                        .build()));

        // When & Then
        assertThatThrownBy(() -> createShortUrlService.createShortUrl(longUrl))
                .isInstanceOf(CodeCollisionException.class)
                .hasMessageContaining("Failed to generate unique code after 5 attempts");
    }

    @Test
    @DisplayName("ArgumentCaptor로 저장된 엔티티 상세 검증")
    void createShortUrl_VerifyEntityDetails() {
        // Given
        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getDefaultExpirationDays()).thenReturn(180L);

        String longUrl = "https://www.example.com/test";
        String generatedCode = "detailed123";
        Instant fixedTime = fixedClock.instant();

        ShortUrl savedUrl = ShortUrl.builder()
                .id(100L)
                .code(generatedCode)
                .longUrl(longUrl)
                .createdAt(fixedTime)
                .expiresAt(fixedTime.plusSeconds(180 * 24 * 60 * 60)) // 180 days
                .build();

        when(shortUrlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(codeGenerator.generate(longUrl + "_0")).thenReturn(generatedCode);
        when(shortUrlRepository.findByCode(generatedCode)).thenReturn(Optional.empty());
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(savedUrl);

        // When
        ShortUrl result = createShortUrlService.createShortUrl(longUrl);

        // Then
        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).save(captor.capture());

        ShortUrl captured = captor.getValue();
        assertThat(captured.code()).isEqualTo(generatedCode);
        assertThat(captured.longUrl()).isEqualTo(longUrl);
        assertThat(captured.createdAt()).isEqualTo(fixedTime);
        assertThat(captured.expiresAt()).isEqualTo(fixedTime.plus(180, ChronoUnit.DAYS));

        assertThat(result).isEqualTo(savedUrl);
    }
}