package io.github.columnwise.shortlink.adapter.web;

import io.github.columnwise.shortlink.domain.exception.UrlNotFoundException;
import io.github.columnwise.shortlink.domain.exception.CodeCollisionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * 글로벌 예외 처리 핸들러
 *
 * <p>애플리케이션 전역에서 발생하는 예외를 처리하고 일관된 에러 응답을 제공합니다.
 * 각 예외 유형에 따라 적절한 HTTP 상태 코드와 에러 메시지를 반환합니다.</p>
 *
 * <p>처리하는 예외 유형:</p>
 * <ul>
 *   <li>{@link UrlNotFoundException} - 404 Not Found</li>
 *   <li>{@link MethodArgumentNotValidException} - 400 Bad Request (유효성 검증 실패)</li>
 *   <li>{@link IllegalArgumentException} - 400 Bad Request (잘못된 인자)</li>
 *   <li>{@link MethodArgumentTypeMismatchException} - 400 Bad Request (타입 불일치)</li>
 *   <li>{@link Exception} - 500 Internal Server Error (기타 모든 예외)</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * URL 조회 실패 예외를 처리합니다.
     *
     * @param ex UrlNotFoundException 예외
     * @return 404 Not Found 응답
     */
    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUrlNotFoundException(UrlNotFoundException ex) {
        log.warn("[404] URL not found: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "URL_NOT_FOUND");
        error.put("message", "The requested URL was not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 코드 충돌 예외를 처리합니다.
     *
     * <p>단축 코드 생성 시 최대 재시도 후에도 고유한 코드를 생성하지 못한 경우 발생합니다.
     * 일시적인 시스템 과부하 상태로 간주하여 503 Service Unavailable을 반환합니다.</p>
     *
     * @param ex CodeCollisionException 예외
     * @return 503 Service Unavailable 응답
     */
    @ExceptionHandler(CodeCollisionException.class)
    public ResponseEntity<Map<String, String>> handleCodeCollisionException(CodeCollisionException ex) {
        log.error("[503] Code collision occurred: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "SERVICE_UNAVAILABLE");
        error.put("message", "Service is temporarily unavailable. Please try again later.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * 요청 데이터 유효성 검증 실패 예외를 처리합니다.
     *
     * @param ex MethodArgumentNotValidException 예외
     * @return 400 Bad Request 응답 (필드별 유효성 오류 포함)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("[400] Validation failed with {} errors", ex.getBindingResult().getErrorCount());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * 잘못된 인자 예외를 처리합니다.
     *
     * @param ex IllegalArgumentException 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("[400] IllegalArgumentException", ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "BAD_REQUEST");
        error.put("message", "Invalid request parameters");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 메서드 인자 타입 불일치 예외를 처리합니다.
     *
     * @param ex MethodArgumentTypeMismatchException 예외
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("[400] MethodArgumentTypeMismatchException for parameter: {}", ex.getName(), ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "BAD_REQUEST");
        error.put("message", "Invalid parameter format");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 처리되지 않은 모든 예외를 처리합니다.
     *
     * <p>예상하지 못한 시스템 오류에 대해 일반적인 500 에러 응답을 제공합니다.
     * 보안상 내부 오류 세부사항은 노출하지 않습니다.</p>
     *
     * @param ex Exception 예외
     * @return 500 Internal Server Error 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("[500] Unexpected error occurred: {}", ex.getMessage(), ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "INTERNAL_SERVER_ERROR");
        error.put("message", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
