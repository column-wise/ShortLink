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
 * <p>허용되는 안전한 스킴:</p>
 * <ul>
 *   <li>http</li>
 *   <li>https</li>
 * </ul>
 */
@Slf4j
public class UriSchemeValidator {

    /**
     * 허용되는 안전한 URI 스킴 목록
     */
    private static final Set<String> ALLOWED_SCHEMES = Set.of(
            "http",
            "https"
    );

    /**
     * 차단해야 할 위험한 URI 스킴 목록 (로깅용)
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
            "ms-appx-web",
            "ftp",
            "sftp",
            "mailto",
            "tel",
            "sms"
    );

    /**
     * URI의 스킴이 안전한지 검증합니다.
     *
     * @param url 검증할 URL
     * @return 안전한 스킴이면 true, 그렇지 않으면 false
     */
    public static boolean isValidScheme(String url) {
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
            boolean isAllowed = ALLOWED_SCHEMES.contains(lowerScheme);

            if (!isAllowed) {
                log.warn("Unknown or disallowed scheme: {} in URL: {}", lowerScheme, url);
            }

            return isAllowed;

        } catch (URISyntaxException e) {
            log.warn("Invalid URI syntax: {} - {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * 허용되는 스킴 목록을 반환합니다.
     *
     * @return 허용되는 스킴의 불변 집합
     */
    public static Set<String> getAllowedSchemes() {
        return ALLOWED_SCHEMES;
    }
}