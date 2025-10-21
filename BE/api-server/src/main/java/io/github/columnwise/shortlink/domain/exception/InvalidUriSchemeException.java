package io.github.columnwise.shortlink.domain.exception;

/**
 * 허용되지 않은 URI 스킴 예외
 */
public class InvalidUriSchemeException extends RuntimeException {
    public InvalidUriSchemeException(String message) {
        super(message);
    }
}

