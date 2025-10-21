package io.github.columnwise.shortlink.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 로드밸런서 환경에서 클라이언트 정보 추출 유틸리티
 */
public final class ClientInfoExtractor {

    private ClientInfoExtractor() {
        // 유틸리티 클래스는 인스턴스화 방지
    }

    /**
     * 로드밸런서를 고려한 실제 클라이언트 IP 추출
     *
     * @param request HTTP 요청
     * @return 실제 클라이언트 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For 헤더 (가장 일반적)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 첫 번째 IP가 실제 클라이언트 IP (체인된 프록시의 경우)
            return xForwardedFor.split(",")[0].trim();
        }

        // X-Real-IP 헤더 (Nginx 등)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        // CF-Connecting-IP 헤더 (Cloudflare)
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isEmpty() && !"unknown".equalsIgnoreCase(cfConnectingIp)) {
            return cfConnectingIp;
        }

        // X-Cluster-Client-IP 헤더 (일부 클러스터)
        String xClusterClientIp = request.getHeader("X-Cluster-Client-IP");
        if (xClusterClientIp != null && !xClusterClientIp.isEmpty() && !"unknown".equalsIgnoreCase(xClusterClientIp)) {
            return xClusterClientIp;
        }

        // 기본값: 직접 연결된 IP
        return request.getRemoteAddr();
    }

    /**
     * User-Agent에서 브라우저 정보 추출
     *
     * @param userAgent User-Agent 문자열
     * @return 브라우저 패밀리 (Chrome, Safari, Firefox 등)
     */
    public static String extractBrowserFamily(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        // 순서 중요: 더 구체적인 것부터 확인
        if (ua.contains("edg")) return "Edge";
        if (ua.contains("opr") || ua.contains("opera")) return "Opera";
        if (ua.contains("chrome")) return "Chrome";
        if (ua.contains("safari")) return "Safari";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("msie") || ua.contains("trident")) return "IE";

        return "Other";
    }

    /**
     * User-Agent에서 디바이스 타입 추출
     *
     * @param userAgent User-Agent 문자열
     * @return 디바이스 타입 (Mobile, Tablet, Desktop)
     */
    public static String extractDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        // 모바일 디바이스 패턴
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") ||
            ua.contains("blackberry") || ua.contains("windows phone")) {
            return "Mobile";
        }

        // 태블릿 패턴
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        }

        // 기본값: 데스크톱
        return "Desktop";
    }
}