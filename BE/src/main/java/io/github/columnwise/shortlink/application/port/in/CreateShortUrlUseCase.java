package io.github.columnwise.shortlink.application.port.in;

import io.github.columnwise.shortlink.domain.model.ShortUrl;

public interface CreateShortUrlUseCase {
    ShortUrl createShortUrl(String longUrl);
}
