package io.github.columnwise.shortlink.domain.exception;

public class UrlNotFoundException extends RuntimeException {
	public UrlNotFoundException(String message) {
		super(message);
	}
}
