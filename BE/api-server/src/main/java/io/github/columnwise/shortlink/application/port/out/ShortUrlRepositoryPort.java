package io.github.columnwise.shortlink.application.port.out;

import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.model.UrlAccessLog;

import java.util.List;
import java.util.Optional;

public interface ShortUrlRepositoryPort {
    ShortUrl save(ShortUrl shortUrl);
    Optional<ShortUrl> findByCode(String code);
    Optional<ShortUrl> findByLongUrl(String longUrl);
    void saveAccessLog(UrlAccessLog accessLog);
    List<UrlAccessLog> findAccessLogsByCode(String code);
}
