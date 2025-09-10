package io.github.columnwise.shortlink.application.port.out;

import io.github.columnwise.shortlink.domain.model.ShortUrl;

import java.util.Optional;

public interface CachePort {
    
    /**
     * 캐시에서 단축 URL 조회
     */
    Optional<ShortUrl> findByCode(String code);
    
    /**
     * 단축 URL을 캐시에 저장
     */
    void save(ShortUrl shortUrl);
    
    /**
     * 캐시에서 단축 URL 삭제
     */
    void delete(String code);
    
    /**
     * 캐시 만료 시간 설정 (초 단위)
     */
    void setExpiration(String code, long seconds);
}
