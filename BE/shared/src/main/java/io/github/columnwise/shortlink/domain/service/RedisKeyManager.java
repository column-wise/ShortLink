package io.github.columnwise.shortlink.domain.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis Cluster 호환을 위한 키 관리 유틸리티
 * Hash Tag를 사용하여 관련 키들을 같은 슬롯에 배치
 */
public class RedisKeyManager {

    // Hash Tag를 사용하여 같은 날짜 데이터를 같은 슬롯에 배치
    // url:hourly:access:{2025-09-15}:example
    private static final String HOURLY_ACCESS_KEY_TEMPLATE = "url:hourly:access:{%s}:%s";
    private static final String DAILY_UNIQUE_kEY_TEMPLATE = "url:daily:unique:{%s}:%s";
    private static final String DAILY_USER_AGENT_kEY_TEMPLATE = "url:daily:ua:{%s}:%s";
    private static final String DAILY_DEVICE_kEY_TEMPLATE = "url:daily:device:{%s}:%s";

    public static String getHourlyAccessKey(String code, LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(HOURLY_ACCESS_KEY_TEMPLATE, dateKey, code);
    }

    public static String getDailyUniqueKey(String code, LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(DAILY_UNIQUE_kEY_TEMPLATE, dateKey, code);
    }

    public static String getDailyUaKey(String code, LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(DAILY_USER_AGENT_kEY_TEMPLATE, dateKey, code);
    }

    public static String getDailyDeviceKey(String code, LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(DAILY_DEVICE_kEY_TEMPLATE, dateKey, code);
    }
    
    /**
     * Redis 키에서 코드를 추출하는 유틸리티 메소드
     * 다양한 키 형태를 지원하여 클러스터 및 단일 Redis 환경 모두 호환
     * 
     * @param key Redis 키
     * @return 추출된 코드, 파싱 실패 시 원본 키 반환
     */
    public static String extractCodeFromKey(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        
        // Hash Tag 형태 파싱: url:access:count:{2025-09-12}:abc123 -> abc123
        if (key.contains("{") && key.contains("}")) {
            try {
                // 마지막 ':'부터 끝까지가 코드
                int lastColonIndex = key.lastIndexOf(':');
                if (lastColonIndex > 0 && lastColonIndex < key.length() - 1) {
                    return key.substring(lastColonIndex + 1);
                }
            } catch (Exception e) {
                // 파싱 실패 시 다음 방식 시도
            }
        }
        
        // 기존 형태 파싱: hitcount:abc123 또는 shortlink:abc123:access -> abc123
        String[] parts = key.split(":");
        if (parts.length >= 2) {
            // 일반적으로 두 번째 부분이 코드
            return parts[1];
        }
        
        // 파싱 실패 시 원본 키 반환
        return key;
    }
    
    /**
     * Redis 키에서 날짜를 추출하는 유틸리티 메소드
     * 
     * @param key Redis 키
     * @return 추출된 LocalDate, 파싱 실패 시 현재 날짜 반환
     */
    public static LocalDate extractDateFromKey(String key) {
        if (key == null || key.isEmpty()) {
            return LocalDate.now();
        }
        
        try {
            // Hash Tag 형태에서 날짜 추출: {2025-09-12}
            if (key.contains("{") && key.contains("}")) {
                int start = key.indexOf('{');
                int end = key.indexOf('}');
                if (start >= 0 && end > start) {
                    String dateStr = key.substring(start + 1, end);
                    return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                }
            }
            
            // 콜론으로 분리된 형태에서 날짜 패턴 찾기
            String[] parts = key.split(":");
            for (String part : parts) {
                if (part.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return LocalDate.parse(part, DateTimeFormatter.ISO_LOCAL_DATE);
                }
            }
        } catch (Exception e) {
            // 파싱 실패 시 현재 날짜 반환
        }
        
        return LocalDate.now();
    }
    
    /**
     * 키가 Hash Tag 형태인지 확인
     * 
     * @param key Redis 키
     * @return Hash Tag 형태이면 true
     */
    public static boolean isHashTagKey(String key) {
        return key != null && key.contains("{") && key.contains("}");
    }
}