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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        verify(shortUrlRepository).findByCode(generatedCode);
        verify(shortUrlRepository).save(any(ShortUrl.class));
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
        when(shortUrlRepository.findByCode(firstCode)).thenReturn(Optional.of(existingUrl));
        when(shortUrlRepository.findByCode(secondCode)).thenReturn(Optional.empty());
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(savedUrl);

        // When
        ShortUrl result = createShortUrlService.createShortUrl(longUrl);

        // Then
        assertThat(result).isEqualTo(savedUrl);
        verify(codeGenerator).generate(longUrl + "_0");
        verify(codeGenerator).generate(longUrl + "_1");
        verify(shortUrlRepository).findByCode(firstCode);
        verify(shortUrlRepository).findByCode(secondCode);
        verify(shortUrlRepository).save(any(ShortUrl.class));
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

        // When & Then
        assertThatThrownBy(() -> createShortUrlService.createShortUrl(longUrl))
                .isInstanceOf(CodeCollisionException.class)
                .hasMessageContaining("Failed to generate unique code");
        
        verify(codeGenerator, times(5)).generate(anyString());
        verify(shortUrlRepository, times(5)).findByCode(code);
        verify(shortUrlRepository, never()).save(any());
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
                .isInstanceOf(CodeCollisionException.class)
                .hasCause(originalException)
                .hasMessageContaining("Failed to generate unique code after 5 attempts");
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
}