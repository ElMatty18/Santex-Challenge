package io.github.elmatty18.workflowinbox.shared.error;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps exceptions to RFC 9457 Problem Details with an {@code errorCode} property.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
		ResponseEntity<ProblemDetail> response = problem(ex.errorCode(), ex.getMessage());
		ex.properties().forEach(response.getBody()::setProperty);
		return response;
	}

	private static ResponseEntity<ProblemDetail> problem(ErrorCode errorCode, String detail) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);
		body.setProperty("errorCode", errorCode.name());
		return ResponseEntity.status(errorCode.status()).body(body);
	}

}
