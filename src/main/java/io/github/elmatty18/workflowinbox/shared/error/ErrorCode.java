package io.github.elmatty18.workflowinbox.shared.error;

import org.springframework.http.HttpStatus;

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
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus status() {
		return status;
	}

}
