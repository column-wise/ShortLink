package io.github.columnwise.shortlink.application.port.out;

import java.time.Duration;
import java.util.function.Supplier;

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
    
    /**
     * 락을 획득하고 작업을 실행하는 템플릿 메서드
     * @param key 락 키
     * @param expiration 락 만료 시간
     * @param action 실행할 작업
     * @return 작업 결과
     * @throws RuntimeException 락 획득 실패 시
     */
    default <T> T withLock(String key, Duration expiration, Supplier<T> action) {
        if (!tryLock(key, expiration)) {
            throw new RuntimeException("Failed to acquire lock for key: " + key);
        }
        try {
            return action.get();
        } finally {
            unlock(key);
        }
    }
    
    /**
     * 락을 획득하고 작업을 실행하는 템플릿 메서드 (반환값 없음)
     * @param key 락 키
     * @param expiration 락 만료 시간
     * @param action 실행할 작업
     * @throws RuntimeException 락 획득 실패 시
     */
    default void withLock(String key, Duration expiration, Runnable action) {
        if (!tryLock(key, expiration)) {
            throw new RuntimeException("Failed to acquire lock for key: " + key);
        }
        try {
            action.run();
        } finally {
            unlock(key);
        }
    }
}
