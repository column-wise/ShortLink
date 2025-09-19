package io.github.columnwise.shortlink.adapter.persistence.repository;

import io.github.columnwise.shortlink.adapter.persistence.entity.UrlMetricsHourlyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UrlMetricsHourlyRepository extends JpaRepository<UrlMetricsHourlyEntity, Long> {

    /**
     * 특정 코드와 날짜의 시간별 통계를 모두 조회합니다.
     */
    List<UrlMetricsHourlyEntity> findByCodeAndDay(String code, LocalDate day);

    /**
     * 특정 날짜의 모든 시간별 통계를 조회합니다.
     */
    List<UrlMetricsHourlyEntity> findByDay(LocalDate day);

    /**
     * 특정 날짜의 특정 코드들의 시간별 통계를 조회합니다.
     */
    @Query("SELECT h FROM UrlMetricsHourlyEntity h WHERE h.code IN :codes AND h.day = :day")
    List<UrlMetricsHourlyEntity> findByCodesAndDay(@Param("codes") List<String> codes, @Param("day") LocalDate day);

    /**
     * 특정 날짜 이전의 데이터를 삭제합니다. (정리용)
     */
    void deleteByDayBefore(LocalDate day);

    /**
     * 특정 코드와 날짜의 PV 합계를 조회합니다.
     */
    @Query("SELECT COALESCE(SUM(h.accesses), 0) FROM UrlMetricsHourlyEntity h WHERE h.code = :code AND h.day = :day")
    long sumAccessesByCodeAndDay(@Param("code") String code, @Param("day") LocalDate day);
}