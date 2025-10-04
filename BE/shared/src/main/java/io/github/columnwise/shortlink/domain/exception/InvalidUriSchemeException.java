package io.github.columnwise.shortlink.domain.exception;

/**
 * 유효하지 않은 URI 스킴 예외
 *
 * <p>허용되지 않은 스킴(javascript:, data: 등)을 가진 URL에 대해 발생하는 예외입니다.</p>
 * <p>보안상 위험하거나 정책적으로 허용되지 않는 URI 스킴을 사용할 때 던져집니다.</p>
 */
public class InvalidUriSchemeException extends RuntimeException {

    public InvalidUriSchemeException(String message) {
        super(message);
    }

    public InvalidUriSchemeException(String message, Throwable cause) {
        super(message, cause);
    }
}