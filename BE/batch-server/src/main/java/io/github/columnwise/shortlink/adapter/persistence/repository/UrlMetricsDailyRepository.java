package io.github.columnwise.shortlink.adapter.persistence.repository;

import io.github.columnwise.shortlink.adapter.persistence.entity.UrlMetricsDailyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlMetricsDailyRepository extends JpaRepository<UrlMetricsDailyEntity, Long> {

    /**
     * 특정 코드와 날짜의 일별 통계를 조회합니다.
     */
    Optional<UrlMetricsDailyEntity> findByCodeAndDay(String code, LocalDate day);

    /**
     * 특정 날짜의 모든 일별 통계를 조회합니다.
     */
    List<UrlMetricsDailyEntity> findByDay(LocalDate day);

    /**
     * 특정 코드의 날짜 범위별 일별 통계를 조회합니다.
     */
    @Query("SELECT d FROM UrlMetricsDailyEntity d WHERE d.code = :code AND d.day BETWEEN :startDate AND :endDate ORDER BY d.day")
    List<UrlMetricsDailyEntity> findByCodeAndDayBetween(@Param("code") String code, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 특정 날짜 이전의 데이터를 삭제합니다. (정리용)
     */
    void deleteByDayBefore(LocalDate day);

    /**
     * 특정 코드의 모든 일별 통계를 조회합니다.
     */
    List<UrlMetricsDailyEntity> findByCodeOrderByDay(String code);
}