package io.github.columnwise.shortlink.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShortUrlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private RedisTemplate<String, String> redisTemplate;
    
    @MockBean
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("URL 단축 생성부터 리다이렉트까지 전체 플로우 테스트")
    void fullWorkflowTest() throws Exception {
        // Redis Mock 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        String longUrl = "https://www.example.com/very/long/path";
        CreateShortUrlRequest request = new CreateShortUrlRequest(longUrl);

        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.code").value(matchesPattern("^[a-zA-Z0-9]+$")))
                .andExpect(jsonPath("$.shortUrl").exists())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String code = objectMapper.readTree(responseBody).get("code").asText();

        mockMvc.perform(get("/api/v1/r/" + code))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(longUrl));

        // 통계는 배치 처리 후에 조회 가능하므로 API 호출만 테스트
        mockMvc.perform(get("/api/v1/urls/" + code + "/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("동일한 URL에 대해 같은 코드 반환 테스트")
    void sameUrlReturnsSameCodeTest() throws Exception {
        String longUrl = "https://www.google.com";
        CreateShortUrlRequest request = new CreateShortUrlRequest(longUrl);

        MvcResult firstResult = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstCode = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .get("code").asText();
        String secondCode = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                .get("code").asText();

        assert firstCode.equals(secondCode);
    }

    @Test
    @DisplayName("빈 URL 검증 테스트")
    void emptyUrlValidationTest() throws Exception {
        CreateShortUrlRequest request = new CreateShortUrlRequest("");
        
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 단축 코드 접근 테스트")
    void nonExistentCodeTest() throws Exception {
        String nonExistentCode = "nonexistent123";

        mockMvc.perform(get("/api/v1/r/" + nonExistentCode))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/urls/" + nonExistentCode + "/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("여러 번 접근 시 통계 누적 테스트")
    void multipleAccessStatsTest() throws Exception {
        // Redis Mock 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        String longUrl = "https://www.github.com";
        CreateShortUrlRequest request = new CreateShortUrlRequest(longUrl);

        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String code = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("code").asText();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/r/" + code))
                    .andExpect(status().is3xxRedirection());
        }

        mockMvc.perform(get("/api/v1/urls/" + code + "/stats"))
                .andExpect(status().isOk());
    }
}