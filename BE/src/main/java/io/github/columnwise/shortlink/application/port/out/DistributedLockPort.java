package io.github.columnwise.shortlink.application.port.out;

import java.time.Duration;

public interface DistributedLockPort {
    
    /**
     * 분산 락 획득 시도
     * @param key 락 키
     * @param expiration 락 만료 시간
     * @return 락 획득 성공 여부
     */
    boolean tryLock(String key, Duration expiration);
    
    /**
     * 분산 락 해제
     * @param key 락 키
     */
    void unlock(String key);
    
    /**
     * 락이 존재하는지 확인
     * @param key 락 키
     * @return 락 존재 여부
     */
    boolean isLocked(String key);
}
