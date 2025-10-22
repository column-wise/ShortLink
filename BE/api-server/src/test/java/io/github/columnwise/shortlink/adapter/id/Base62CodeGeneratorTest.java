package io.github.columnwise.shortlink.adapter.id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base62CodeGenerator 유닛 테스트
 */
class Base62CodeGeneratorTest {

    private Base62CodeGenerator codeGenerator;

    @BeforeEach
    void setUp() {
        codeGenerator = new Base62CodeGenerator();
    }

    @Test
    @DisplayName("동일한 URL에 대해 동일한 코드 생성")
    void testDeterministicGeneration() {
        // Given
        String url = "https://example.com/very/long/path";

        // When
        String code1 = codeGenerator.generate(url);
        String code2 = codeGenerator.generate(url);

        // Then
        assertEquals(code1, code2, "Same URL should always generate the same code");
    }

    @Test
    @DisplayName("다른 URL에 대해 다른 코드 생성")
    void testDifferentUrlsDifferentCodes() {
        // Given
        String url1 = "https://example.com/path1";
        String url2 = "https://example.com/path2";

        // When
        String code1 = codeGenerator.generate(url1);
        String code2 = codeGenerator.generate(url2);

        // Then
        assertNotEquals(code1, code2, "Different URLs should generate different codes");
    }

    @Test
    @DisplayName("생성된 코드의 길이가 6-10자리인지 검증")
    void testCodeLength() {
        // Given
        String[] testUrls = {
            "https://example.com",
            "https://www.google.com/search?q=test",
            "https://github.com/user/repo/issues/123",
            "https://very.long.domain.name.example.com/path/to/resource"
        };

        for (String url : testUrls) {
            // When
            String code = codeGenerator.generate(url);

            // Then
            assertTrue(code.length() >= 6, "Code should be at least 6 characters: " + code);
            assertTrue(code.length() <= 10, "Code should be at most 10 characters: " + code);
        }
    }

    @Test
    @DisplayName("생성된 코드가 Base62 문자만 포함하는지 검증")
    void testCodeContainsOnlyBase62Characters() {
        // Given
        String url = "https://example.com/test";

        // When
        String code = codeGenerator.generate(url);

        // Then
        assertTrue(code.matches("[0-9A-Za-z]+"),
            "Code should contain only Base62 characters: " + code);
    }

    @Test
    @DisplayName("빈 문자열과 null에 대한 처리")
    void testEdgeCases() {
        // Given & When & Then
        assertDoesNotThrow(() -> codeGenerator.generate(""));
        assertThrows(NullPointerException.class, () -> codeGenerator.generate(null));
    }

    @Test
    @DisplayName("매우 긴 URL에 대한 코드 생성")
    void testVeryLongUrl() {
        // Given
        String longUrl = "https://example.com/" + "a".repeat(1000);

        // When
        String code = codeGenerator.generate(longUrl);

        // Then
        assertNotNull(code);
        assertTrue(code.length() >= 6);
        assertTrue(code.length() <= 10);
        assertTrue(code.matches("[0-9A-Za-z]+"));
    }

    @Test
    @DisplayName("특수 문자가 포함된 URL에 대한 코드 생성")
    void testUrlWithSpecialCharacters() {
        // Given
        String urlWithSpecialChars = "https://example.com/path?param=value&other=한글#fragment";

        // When
        String code = codeGenerator.generate(urlWithSpecialChars);

        // Then
        assertNotNull(code);
        assertTrue(code.length() >= 6);
        assertTrue(code.length() <= 10);
        assertTrue(code.matches("[0-9A-Za-z]+"));
    }

    @Test
    @DisplayName("Salt 추가시 다른 코드 생성 확인")
    void testSaltGeneration() {
        // Given
        String baseUrl = "https://example.com";

        // When
        String code1 = codeGenerator.generate(baseUrl + "_0");
        String code2 = codeGenerator.generate(baseUrl + "_1");

        // Then
        assertNotEquals(code1, code2, "Different salts should generate different codes");
    }
}