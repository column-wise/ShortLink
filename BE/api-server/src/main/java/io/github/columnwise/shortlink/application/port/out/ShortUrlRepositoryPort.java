package io.github.columnwise.shortlink.application.port.out;

import io.github.columnwise.shortlink.domain.model.ShortUrl;

import java.util.Optional;

public interface ShortUrlRepositoryPort {
    ShortUrl save(ShortUrl shortUrl);
    Optional<ShortUrl> findByCode(String code);
    Optional<ShortUrl> findByLongUrl(String longUrl);
}
