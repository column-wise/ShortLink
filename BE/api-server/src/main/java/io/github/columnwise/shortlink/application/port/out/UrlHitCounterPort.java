package io.github.columnwise.shortlink.application.port.out;

public interface UrlHitCounterPort {
    
    /**
     * URL 조회 횟수 증가
     */
    void incrementHitCount(String code);
    
    /**
     * URL 조회 횟수 조회
     */
    long getHitCount(String code);
    
    /**
     * URL 조회 횟수 초기화
     */
    void resetHitCount(String code);
}
