package io.github.columnwise.shortlink.domain.exception;

public class CodeCollisionException extends RuntimeException {
	public CodeCollisionException(String message) {
		super(message);
	}
}
