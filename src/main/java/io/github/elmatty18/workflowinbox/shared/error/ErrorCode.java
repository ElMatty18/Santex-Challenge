package io.github.elmatty18.workflowinbox.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Stable, machine-readable error codes returned in the {@code errorCode} property of every
 * Problem Details response (SPEC section 10).
 */
public enum ErrorCode {

	VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
	FORBIDDEN(HttpStatus.FORBIDDEN),
	ACTION_NOT_FOUND(HttpStatus.NOT_FOUND),
	INVALID_TRANSITION(HttpStatus.CONFLICT),
	ACTION_ALREADY_COMPLETED(HttpStatus.CONFLICT),
	CONCURRENT_MODIFICATION(HttpStatus.CONFLICT),
	BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

	// Protocol-level errors raised by Spring MVC itself, not by the application
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
	NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
	REQUEST_NOT_SUPPORTED(HttpStatus.BAD_REQUEST);

	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus status() {
		return status;
	}

	/**
	 * Error code for a Spring MVC exception that only carries a status.
	 */
	static ErrorCode forFrameworkStatus(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> VALIDATION_FAILED;
			case 404 -> RESOURCE_NOT_FOUND;
			case 405 -> METHOD_NOT_ALLOWED;
			case 406 -> NOT_ACCEPTABLE;
			case 415 -> UNSUPPORTED_MEDIA_TYPE;
			default -> status.is5xxServerError() ? INTERNAL_ERROR : REQUEST_NOT_SUPPORTED;
		};
	}

}
