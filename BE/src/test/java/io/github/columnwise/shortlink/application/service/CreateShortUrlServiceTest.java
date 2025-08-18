package io.github.columnwise.shortlink.application.service;

import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.service.CodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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

    private CreateShortUrlService createShortUrlService;

    @BeforeEach
    void setUp() {
        createShortUrlService = new CreateShortUrlService(shortUrlRepository, codeGenerator);
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
        verifyNoMoreInteractions(codeGenerator, shortUrlRepository);
    }

    @Test
    @DisplayName("새 URL 생성 성공")
    void createShortUrl_NewUrl_Success() {
        // Given
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
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate unique code");
        
        verify(codeGenerator, times(5)).generate(anyString());
        verify(shortUrlRepository, times(5)).findByCode(code);
        verify(shortUrlRepository, never()).save(any());
    }

    @Test
    @DisplayName("저장 중 예외 발생 시 재시도")
    void createShortUrl_SaveException_Retry() {
        // Given
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
                .thenThrow(new RuntimeException("DB error"))
                .thenReturn(savedUrl);

        // When
        ShortUrl result = createShortUrlService.createShortUrl(longUrl);

        // Then
        assertThat(result).isEqualTo(savedUrl);
        verify(shortUrlRepository, times(2)).save(any(ShortUrl.class));
    }
}