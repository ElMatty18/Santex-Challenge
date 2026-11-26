package io.github.elmatty18.workflowinbox.shared.error;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.elmatty18.workflowinbox.shared.web.RequestIdFilter;
import tools.jackson.core.JacksonException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps every exception to RFC 9457 Problem Details with {@code errorCode} and {@code requestId}.
 * Extends {@link ResponseEntityExceptionHandler} so Spring MVC's own errors (unknown route, 405,
 * 415, ...) keep their status instead of falling into the 500 handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	static final String VALIDATION_DETAIL = "The request is not valid";

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<Object> handleApiException(ApiException ex) {
		log.info("Request rejected: {} {}", ex.errorCode(), ex.getMessage());
		ProblemDetail body = problem(ex.errorCode(), ex.getMessage());
		ex.properties().forEach(body::setProperty);
		return ResponseEntity.status(ex.errorCode().status()).body(body);
	}

	/**
	 * Logged with the entity and id only: the exception message contains the SQL statement.
	 */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException ex) {
		if (ex instanceof ObjectOptimisticLockingFailureException entity) {
			String type = entity.getPersistentClassName();
			log.info("Request rejected: {} on {} {}", ErrorCode.CONCURRENT_MODIFICATION,
					type.substring(type.lastIndexOf('.') + 1), entity.getIdentifier());
		}
		else {
			log.info("Request rejected: {}", ErrorCode.CONCURRENT_MODIFICATION);
		}
		return respond(problem(ErrorCode.CONCURRENT_MODIFICATION,
				"The resource was modified by another request; reload it and retry"));
	}

	/**
	 * Anything unexpected: logged in full (with the request id from the MDC), answered with a
	 * generic body. Neither the exception message nor the stack trace reaches the caller.
	 */
	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return respond(problem(ErrorCode.INTERNAL_ERROR,
				"An unexpected error occurred. Quote the requestId when reporting it."));
	}

	/** Invalid {@code @Valid @RequestBody}. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
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
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
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
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName()
				: ex.getPropertyName();
		return validationProblem(List.of(new FieldErrorDetail(name, "Invalid value '%s'".formatted(ex.getValue()))));
	}

	/** Malformed JSON, or a value that cannot be mapped (e.g. an unknown enum constant). */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		if (ex.getMostSpecificCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
			String field = jackson.getPath()
				.stream()
				.map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
				.collect(Collectors.joining("."));
			return validationProblem(List.of(new FieldErrorDetail(field, "Invalid value")));
		}
		return respond(problem(ErrorCode.VALIDATION_FAILED, "Malformed JSON request body"));
	}

	/**
	 * Every response built by {@link ResponseEntityExceptionHandler} passes here: add the same
	 * properties as the application's own errors.
	 */
	@Override
	protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
			WebRequest request) {
		if (body instanceof ProblemDetail problem) {
			if (problem.getProperties() == null || !problem.getProperties().containsKey("errorCode")) {
				problem.setProperty("errorCode", ErrorCode.forFrameworkStatus(statusCode).name());
			}
			problem.setProperty("requestId", MDC.get(RequestIdFilter.MDC_REQUEST_ID));
		}
		return super.createResponseEntity(body, headers, statusCode, request);
	}

	private static ResponseEntity<Object> validationProblem(List<FieldErrorDetail> fieldErrors) {
		ProblemDetail body = problem(ErrorCode.VALIDATION_FAILED, VALIDATION_DETAIL);
		body.setProperty("fieldErrors", fieldErrors);
		return respond(body);
	}

	private static ResponseEntity<Object> respond(ProblemDetail body) {
		return ResponseEntity.status(body.getStatus()).body(body);
	}

	private static ProblemDetail problem(ErrorCode errorCode, String detail) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);
		body.setProperty("errorCode", errorCode.name());
		body.setProperty("requestId", MDC.get(RequestIdFilter.MDC_REQUEST_ID));
		return body;
	}

}
