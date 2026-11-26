package io.github.elmatty18.workflowinbox.shared.error;

/**
 * Expected business or client error. The message is safe to return to the caller as the
 * Problem Details {@code detail}.
 */
public class ApiException extends RuntimeException {

	private final ErrorCode errorCode;

	public ApiException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}

}
