package io.github.elmatty18.workflowinbox.shared.error;

import java.util.Map;

/**
 * Expected business or client error. The message is safe to return to the caller as the
 * Problem Details {@code detail}; {@link #properties()} are added to the body as extra members.
 */
public class ApiException extends RuntimeException {

	private final ErrorCode errorCode;

	private final Map<String, Object> properties;

	public ApiException(ErrorCode errorCode, String message) {
		this(errorCode, message, Map.of());
	}

	public ApiException(ErrorCode errorCode, String message, Map<String, Object> properties) {
		super(message);
		this.errorCode = errorCode;
		this.properties = Map.copyOf(properties);
	}

	public ErrorCode errorCode() {
		return errorCode;
	}

	public Map<String, Object> properties() {
		return properties;
	}

}
