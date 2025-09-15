package io.github.columnwise.shortlink.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlRequest;
import io.github.columnwise.shortlink.application.port.in.CreateShortUrlUseCase;
import io.github.columnwise.shortlink.application.port.in.GetStatsUseCase;
import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.model.UrlMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShortUrlController.class)
@ActiveProfiles("test")
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateShortUrlUseCase createShortUrlUseCase;

    @MockitoBean
    private ResolveUrlUseCase resolveUrlUseCase;

    @MockitoBean
    private GetStatsUseCase getStatsUseCase;

    @Test
    @DisplayName("URL 단축 생성 성공")
    void createShortUrl_Success() throws Exception {
        // Given
        String longUrl = "https://www.example.com";
        String shortCode = "abc123";
        CreateShortUrlRequest request = new CreateShortUrlRequest(longUrl);
        
        ShortUrl mockShortUrl = ShortUrl.builder()
                .id(1L)
                .code(shortCode)
                .longUrl(longUrl)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(31536000)) // 1년
                .build();

        when(createShortUrlUseCase.createShortUrl(eq(longUrl))).thenReturn(mockShortUrl);

        // When & Then
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(shortCode))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/api/v1/r/" + shortCode));
    }

    @Test
    @DisplayName("잘못된 URL로 단축 생성 실패")
    void createShortUrl_InvalidUrl() throws Exception {
        // Given
        CreateShortUrlRequest request = new CreateShortUrlRequest("");

        // When & Then
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("단축 URL 리다이렉트 성공")
    void redirectToOriginalUrl_Success() throws Exception {
        // Given
        String code = "abc123";
        String longUrl = "https://www.example.com";

        when(resolveUrlUseCase.resolveUrl(eq(code), any(String.class), any(String.class), any(String.class))).thenReturn(longUrl);

        // When & Then
        mockMvc.perform(get("/api/v1/r/" + code))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(longUrl));
    }

    @Test
    @DisplayName("존재하지 않는 코드로 리다이렉트 실패")
    void redirectToOriginalUrl_NotFound() throws Exception {
        // Given
        String code = "notfound";

        when(resolveUrlUseCase.resolveUrl(eq(code), any(String.class), any(String.class), any(String.class)))
                .thenThrow(new UrlNotFoundException("URL not found for code: " + code));

        // When & Then
        mockMvc.perform(get("/api/v1/r/" + code))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("URL 메트릭 조회 성공")
    void getUrlMetrics_Success() throws Exception {
        // Given
        String code = "abc123";
        UrlMetrics mockMetrics = UrlMetrics.builder()
                .code(code)
                .totalAccesses(150L)
                .build();

        when(getStatsUseCase.getUrlMetrics(eq(code))).thenReturn(mockMetrics);

        // When & Then
        mockMvc.perform(get("/api/v1/urls/" + code + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.totalAccesses").value(150));
    }
}