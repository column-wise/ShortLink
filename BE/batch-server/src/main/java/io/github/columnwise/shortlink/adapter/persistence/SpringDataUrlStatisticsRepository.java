package io.github.columnwise.shortlink.adapter.persistence;

import io.github.columnwise.shortlink.domain.model.UrlStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SpringDataUrlStatisticsRepository extends JpaRepository<UrlStatisticsEntity, Long> {
    
    Optional<UrlStatisticsEntity> findByCode(String code);
    
    @Modifying
    @Query("UPDATE UrlStatisticsEntity u SET u.totalAccessCount = u.totalAccessCount + :increment, u.updatedAt = :now WHERE u.code = :code")
    int incrementAccessCount(@Param("code") String code, @Param("increment") long increment, @Param("now") Instant now);
    
    @Modifying
    @Query("UPDATE UrlStatisticsEntity u SET u.lastAccessedAt = :accessTime, u.updatedAt = :now WHERE u.code = :code")
    int updateLastAccessTime(@Param("code") String code, @Param("accessTime") Instant accessTime, @Param("now") Instant now);
    
    boolean existsByCode(String code);
}