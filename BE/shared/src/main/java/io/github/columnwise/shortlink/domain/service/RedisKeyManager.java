package io.github.columnwise.shortlink.domain.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis Cluster 호환을 위한 키 관리 유틸리티
 * Hash Tag를 사용하여 관련 키들을 같은 슬롯에 배치
 */
public class RedisKeyManager {
    
    // Hash Tag를 사용하여 같은 날짜 데이터를 같은 슬롯에 배치
    private static final String ACCESS_COUNT_KEY_TEMPLATE = "url:access:count:{%s}:%s";
    private static final String DAILY_STATS_KEY_TEMPLATE = "url:daily:stats:{%s}:%s";
    private static final String TOTAL_ACCESS_KEY_TEMPLATE = "url:total:access:{%s}:%s";
    private static final String LAST_ACCESS_KEY_TEMPLATE = "url:last:access:{%s}:%s";
    
    // 키 목록 관리를 위한 SET
    private static final String ACCESS_CODES_SET_TEMPLATE = "url:access:codes:{%s}";
    private static final String DAILY_CODES_SET_TEMPLATE = "url:daily:codes:{%s}";
    
    // 분산 락
    private static final String BATCH_LOCK_KEY_TEMPLATE = "batch:lock:aggregation:{%s}";
    private static final String PROCESSED_MARKER_TEMPLATE = "batch:processed:{%s}:%s";
    
    public static String getAccessCountKey(String code, LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(ACCESS_COUNT_KEY_TEMPLATE, dateKey, code);
    }
    
    public static String getDailyStatsKey(String code, LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(DAILY_STATS_KEY_TEMPLATE, dateKey, code);
    }
    
    public static String getTotalAccessKey(String code, LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(TOTAL_ACCESS_KEY_TEMPLATE, dateKey, code);
    }
    
    public static String getLastAccessKey(String code, LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(LAST_ACCESS_KEY_TEMPLATE, dateKey, code);
    }
    
    public static String getAccessCodesSetKey(LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(ACCESS_CODES_SET_TEMPLATE, dateKey);
    }
    
    public static String getDailyCodesSetKey(LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(DAILY_CODES_SET_TEMPLATE, dateKey);
    }
    
    public static String getBatchLockKey(LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(BATCH_LOCK_KEY_TEMPLATE, dateKey);
    }
    
    public static String getProcessedMarkerKey(String code, LocalDate date) {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format(PROCESSED_MARKER_TEMPLATE, dateKey, code);
    }
}