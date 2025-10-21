package io.github.columnwise.shortlink.domain.exception;

/**
 * 단축 URL 미존재 예외
 */
public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String message) {
        super(message);
    }
}

