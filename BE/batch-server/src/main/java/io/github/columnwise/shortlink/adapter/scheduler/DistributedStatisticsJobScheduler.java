package io.github.columnwise.shortlink.adapter.scheduler;

import io.github.columnwise.shortlink.domain.service.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class DistributedStatisticsJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job statisticsAggregationJob;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private final String instanceId = generateInstanceId();

    /**
     * 매시간 5분에 통계 집계 배치 실행 (이전 시간 데이터 처리)
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void runHourlyStatisticsAggregation() {
        LocalDate today = LocalDate.now();
        tryRunBatchWithLock("hourly", today);
    }

    /**
     * 매일 자정 10분에 일일 통계 집계 배치 실행 (전날 데이터 재처리)
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void runDailyStatisticsAggregation() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        tryRunBatchWithLock("daily", yesterday);
    }

    private void tryRunBatchWithLock(String batchType, LocalDate targetDate) {
        String lockKey = RedisKeyManager.getBatchLockKey(targetDate) + ":" + batchType;
        
        try {
            // 분산 락 획득 시도 (최대 10분 유지)
            Boolean lockAcquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, instanceId, Duration.ofMinutes(10));
            
            if (Boolean.TRUE.equals(lockAcquired)) {
                log.info("Distributed lock acquired for {} batch on {}", batchType, targetDate);
                
                try {
                    runStatisticsAggregationJob(batchType, targetDate);
                    log.info("{} statistics aggregation completed successfully for {}", 
                            batchType, targetDate);
                    
                } catch (Exception e) {
                    log.error("Failed to run {} statistics aggregation for {}", 
                            batchType, targetDate, e);
                } finally {
                    // 락 해제
                    releaseLock(lockKey);
                }
                
            } else {
                String currentHolder = (String) redisTemplate.opsForValue().get(lockKey);
                log.info("Another instance ({}) is processing {} batch for {}. Skipping.", 
                        currentHolder, batchType, targetDate);
            }
            
        } catch (Exception e) {
            log.error("Error in distributed batch execution for {} on {}", 
                    batchType, targetDate, e);
        }
    }

    private void runStatisticsAggregationJob(String batchType, LocalDate targetDate) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("batchType", batchType)
                .addString("targetDate", targetDate.toString())
                .addString("instanceId", instanceId)
                .toJobParameters();
        
        jobLauncher.run(statisticsAggregationJob, jobParameters);
    }

    private void releaseLock(String lockKey) {
        try {
            // 자신이 획득한 락만 해제
            String script = 
                "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                "  return redis.call('DEL', KEYS[1]) " +
                "else " +
                "  return 0 " +
                "end";
            
            redisTemplate.execute((RedisCallback<Long>) connection -> {
                return connection.eval(
                    script.getBytes(),
                    org.springframework.data.redis.connection.ReturnType.INTEGER,
                    1,
                    lockKey.getBytes(),
                    instanceId.getBytes()
                );
            });
            
            log.debug("Released distributed lock: {}", lockKey);
            
        } catch (Exception e) {
            log.warn("Failed to release lock: {}", lockKey, e);
        }
    }

    private String generateInstanceId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            return hostname + "-" + uuid;
        } catch (Exception e) {
            return "unknown-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}