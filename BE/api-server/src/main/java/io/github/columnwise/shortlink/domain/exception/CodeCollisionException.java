package io.github.columnwise.shortlink.domain.exception;

/**
 * 코드 충돌(고유 제약 위반) 예외
 */
public class CodeCollisionException extends RuntimeException {
    public CodeCollisionException(String message) {
        super(message);
    }

    public CodeCollisionException(String message, Throwable cause) {
        super(message, cause);
    }
}

