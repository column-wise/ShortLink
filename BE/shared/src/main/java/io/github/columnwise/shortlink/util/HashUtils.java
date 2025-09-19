package io.github.columnwise.shortlink.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 해싱 관련 유틸리티 클래스
 */
public final class HashUtils {

    private HashUtils() {
        // 유틸리티 클래스는 인스턴스화 방지
    }

    /**
     * 입력 문자열을 SHA-256으로 해싱하여 hex 문자열을 반환합니다.
     *
     * @param input 해싱할 입력 문자열
     * @return hex 인코딩된 해시값
     */
    public static String sha256(String input) {
        if (input == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}