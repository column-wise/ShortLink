package io.github.columnwise.shortlink.adapter.persistence;

import io.github.columnwise.shortlink.adapter.persistence.entity.UrlAccessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataUrlAccessLogRepository extends JpaRepository<UrlAccessLogEntity, Long> {
    List<UrlAccessLogEntity> findByCodeOrderByAccessedAtDesc(String code);
}