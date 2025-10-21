package io.github.columnwise.shortlink.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * URI 스킴 검증 유틸리티
 *
 * <p>리다이렉트 전에 URL의 스킴을 검증하여 안전한 프로토콜만 허용합니다.
 * 악성 스킴(javascript:, data:, file: 등)을 차단하여 보안을 강화합니다.</p>
 *
 * <p>허용되는 스킴은 ShortUrlProperties 설정을 통해 관리됩니다.</p>
 */
@Slf4j
public class UriSchemeValidator {

    private UriSchemeValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * 차단해야 할 위험한 URI 스킴 목록 (로깅용)
     * 주로 XSS, CSRF, 로컬 파일 접근 등의 보안 위험이 있는 스킴들
     */
    private static final Set<String> DANGEROUS_SCHEMES = Set.of(
            "javascript",
            "data",
            "file",
            "vbscript",
            "about",
            "chrome",
            "chrome-extension",
            "ms-appx",
            "ms-appx-web"
    );

    /**
     * URI의 스킴이 안전한지 검증합니다.
     *
     * @param url 검증할 URL
     * @param allowedSchemes 허용되는 스킴 목록
     * @return 안전한 스킴이면 true, 그렇지 않으면 false
     */
    public static boolean isValidScheme(String url, Set<String> allowedSchemes) {
        if (url == null || url.isBlank()) {
            log.warn("Empty or null URL provided for scheme validation");
            return false;
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();

            if (scheme == null) {
                log.warn("URL without scheme: {}", url);
                return false;
            }

            String lowerScheme = scheme.toLowerCase();

            // 위험한 스킴 체크 및 로깅
            if (DANGEROUS_SCHEMES.contains(lowerScheme)) {
                log.warn("Dangerous scheme detected and blocked: {} in URL: {}", lowerScheme, url);
                return false;
            }

            // 허용된 스킴 체크
            boolean isAllowed = allowedSchemes.contains(lowerScheme);

            if (!isAllowed) {
                log.warn("Unknown or disallowed scheme: {} in URL: {}", lowerScheme, url);
            }

            return isAllowed;

        } catch (URISyntaxException e) {
            log.warn("Invalid URI syntax: {} - {}", url, e.getMessage());
            return false;
        }
    }

}