package io.github.elmatty18.workflowinbox.shared.error;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import tools.jackson.core.JacksonException;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps exceptions to RFC 9457 Problem Details with an {@code errorCode} property.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	static final String VALIDATION_DETAIL = "The request is not valid";

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
		ResponseEntity<ProblemDetail> response = problem(ex.errorCode(), ex.getMessage());
		ex.properties().forEach(response.getBody()::setProperty);
		return response;
	}

	/** Invalid {@code @Valid @RequestBody}. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleInvalidBody(MethodArgumentNotValidException ex) {
		List<FieldErrorDetail> fieldErrors = new ArrayList<>();
		ex.getBindingResult()
			.getFieldErrors()
			.forEach(error -> fieldErrors.add(new FieldErrorDetail(error.getField(), error.getDefaultMessage())));
		ex.getBindingResult()
			.getGlobalErrors()
			.forEach(error -> fieldErrors.add(new FieldErrorDetail(error.getObjectName(), error.getDefaultMessage())));
		return validationProblem(fieldErrors);
	}

	/** Constraint violations on {@code @RequestParam} / {@code @PathVariable}. */
	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ProblemDetail> handleInvalidParameters(HandlerMethodValidationException ex) {
		List<FieldErrorDetail> fieldErrors = ex.getParameterValidationResults()
			.stream()
			.flatMap(result -> result.getResolvableErrors()
				.stream()
				.map(error -> new FieldErrorDetail(result.getMethodParameter().getParameterName(),
						error.getDefaultMessage())))
			.toList();
		return validationProblem(fieldErrors);
	}

	/** A parameter that cannot be converted, e.g. {@code type=FOO} or a non-numeric id. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return validationProblem(List.of(new FieldErrorDetail(ex.getName(), "Invalid value '%s'".formatted(ex.getValue()))));
	}

	/** Malformed JSON, or a value that cannot be mapped (e.g. an unknown enum constant). */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException ex) {
		if (ex.getMostSpecificCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
			String field = jackson.getPath()
				.stream()
				.map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
				.collect(Collectors.joining("."));
			return validationProblem(List.of(new FieldErrorDetail(field, "Invalid value")));
		}
		return problem(ErrorCode.VALIDATION_FAILED, "Malformed JSON request body");
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex) {
		return problem(ErrorCode.CONCURRENT_MODIFICATION,
				"The resource was modified by another request; reload it and retry");
	}

	private static ResponseEntity<ProblemDetail> validationProblem(List<FieldErrorDetail> fieldErrors) {
		ResponseEntity<ProblemDetail> response = problem(ErrorCode.VALIDATION_FAILED, VALIDATION_DETAIL);
		response.getBody().setProperty("fieldErrors", fieldErrors);
		return response;
	}

	private static ResponseEntity<ProblemDetail> problem(ErrorCode errorCode, String detail) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);
		body.setProperty("errorCode", errorCode.name());
		return ResponseEntity.status(errorCode.status()).body(body);
	}

}
