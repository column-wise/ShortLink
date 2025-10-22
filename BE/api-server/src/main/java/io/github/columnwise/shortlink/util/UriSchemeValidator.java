package io.github.columnwise.shortlink.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 차단해야 할 위험한 URI 스킴 목록
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
     * @param url            검증할 URL
     * @param allowedSchemes 허용되는 스킴 목록 (대소문자 무관)
     * @return 안전한 스킴이면 true, 그렇지 않으면 false
     */
    public static boolean isValidScheme(String url, Set<String> allowedSchemes) {
        if (url == null || url.isBlank()) {
            log.warn("Empty or null URL provided for scheme validation");
            return false;
        }

        if (allowedSchemes == null || allowedSchemes.isEmpty()) {
            log.error("Allowed schemes set is null or empty; denying URL: {}", safeUrlSummary(url));
            return false;
        }

        // 호출부 오설정 방지를 위한 소문자 정규화
        final Set<String> normalizedAllowed = allowedSchemes.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();

            if (scheme == null) {
                log.warn("URL without scheme: {}", safeUrlSummary(url));
                return false;
            }

            String lowerScheme = scheme.toLowerCase();

            // 위험한 스킴 체크 및 로깅
            if (DANGEROUS_SCHEMES.contains(lowerScheme)) {
                log.warn("Dangerous scheme detected and blocked: {} url: {}", lowerScheme, safeUrlSummary(url));
                return false;
            }

            // 허용된 스킴 체크
            boolean isAllowed = normalizedAllowed.contains(lowerScheme);

            if (!isAllowed) {
                log.warn("Unknown or disallowed scheme: {} url: {}", lowerScheme, safeUrlSummary(url));
            }

            return isAllowed;

        } catch (URISyntaxException e) {
            log.warn("Invalid URI syntax: {} - {}", safeUrlSummary(url), e.getMessage());
            return false;
        }
    }

    /**
     * 로그용 URL 요약(민감정보 제거).
     * - scheme/host/port/path(앞부분)만 노출
     * - query 값은 숨기고, 키 목록만 출력
     * - 원문 URL은 해시로만 상관관계 추적
     */
    private static String safeUrlSummary(String raw) {
        if (raw == null) return "null";

        try {
            URI u = new URI(raw);

            String scheme = nvl(u.getScheme());
            String host = nvl(u.getHost());
            String port = (u.getPort() == -1 ? "" : String.valueOf(u.getPort()));

            String path = nvl(u.getPath());
            final int MAX_PATH_LEN = 100;
            if (path.length() > MAX_PATH_LEN) {
                path = path.substring(0, MAX_PATH_LEN) + "...";
            }

            String rawQuery = u.getRawQuery();
            boolean hasQuery = rawQuery != null && !rawQuery.isEmpty();
            String queryKeys = "";
            if (hasQuery) {
                queryKeys = java.util.Arrays.stream(rawQuery.split("&"))
                        .map(p -> p.split("=", 2)[0])
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .limit(20)
                        .collect(Collectors.joining(","));
            }

            return String.format(
                    "scheme=%s host=%s port=%s path=%s hasQuery=%s queryKeys=[%s] hash=%s",
                    scheme, host, port, path, hasQuery, queryKeys, maskForLog(raw)
            );
        } catch (Exception e) {
            // 구문 자체가 깨진 경우: 앞부분만 안전하게 보여주고 해시만
            String head = raw.substring(0, Math.min(64, raw.length()));
            return String.format("malformed head='%s' hash=%s", head, maskForLog(raw));
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /**
     * 원문 문자열을 직접 노출하지 않고, 상관관계를 위한 고정 길이 해시를 남김.
     * SHA-256 해시의 앞 16바이트(32 hex)만 사용.
     */
    private static String maskForLog(String input) {
        if (input == null) return "null";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return "***";
        }
    }
}
