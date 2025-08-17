package io.github.columnwise.shortlink.adapter.persistence;

import io.github.columnwise.shortlink.adapter.persistence.entity.ShortUrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataShortUrlRepository extends JpaRepository<ShortUrlEntity, Long> {
    Optional<ShortUrlEntity> findByCode(String code);
    Optional<ShortUrlEntity> findByLongUrl(String longUrl);
}
