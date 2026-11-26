package io.github.elmatty18.workflowinbox.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Error responses as a whole: every one is Problem Details with errorCode and requestId, and
 * unexpected errors are logged in full but answered generically.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import(ErrorHandlingIntegrationTests.FailingController.class)
class ErrorHandlingIntegrationTests {

	private static final String SECRET = "connection string user=sa password=s3cret";

	@Autowired
	private MockMvcTester mvc;

	@Test
	void unexpectedErrorIs500WithGenericBodyAndFullLog(CapturedOutput output) throws Exception {
		String requestId = "boom-" + UUID.randomUUID();

		MvcTestResult result = mvc.get()
			.uri("/api/test/boom")
			.header("X-User-Id", "ana")
			.header("X-Request-Id", requestId)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("INTERNAL_ERROR");
		assertThat(result).bodyJson().extractingPath("$.requestId").isEqualTo(requestId);
		assertThat(result.getResponse().getContentAsString()).doesNotContain(SECRET, "IllegalStateException", "trace",
				"FailingController");
		// the log has everything: message, exception, stack trace, and the request id via the MDC
		assertThat(output).contains("Unexpected error", SECRET, "at io.github.elmatty18.workflowinbox", requestId);
	}

	@Test
	void applicationErrorsCarryTheRequestId() {
		assertProblem(mvc.get().uri("/api/v1/actions"), HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
		assertProblem(mvc.get().uri("/api/v1/actions/999999").header("X-User-Id", "admin"), HttpStatus.NOT_FOUND,
				"ACTION_NOT_FOUND");
		assertProblem(mvc.get().uri("/api/v1/actions?size=0").header("X-User-Id", "admin"), HttpStatus.BAD_REQUEST,
				"VALIDATION_FAILED");
	}

	@Test
	void springMvcErrorsKeepTheirStatusAndBecomeProblemDetails() {
		assertProblem(mvc.get().uri("/api/v1/nothing-here").header("X-User-Id", "admin"), HttpStatus.NOT_FOUND,
				"RESOURCE_NOT_FOUND");
		assertProblem(mvc.delete().uri("/api/v1/actions/1").header("X-User-Id", "admin"), HttpStatus.METHOD_NOT_ALLOWED,
				"METHOD_NOT_ALLOWED");
		assertProblem(mvc.post()
			.uri("/api/v1/actions")
			.header("X-User-Id", "ana")
			.contentType(MediaType.TEXT_PLAIN)
			.content("hello"), HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE");
	}

	@Test
	void logLinesCarryRequestIdAndUserId(CapturedOutput output) {
		String requestId = "log-" + UUID.randomUUID();

		mvc.post()
			.uri("/api/v1/actions")
			.header("X-User-Id", "ana")
			.header("X-Request-Id", requestId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"type": "ONBOARDING", "title": "Log pattern test"}""")
			.exchange();

		assertThat(output.getOut().lines().filter(line -> line.contains("Action ") && line.contains(" created")))
			.anySatisfy(line -> assertThat(line).contains("[" + requestId + " ana] "));
	}

	private void assertProblem(MockMvcTester.MockMvcRequestBuilder request, HttpStatus status, String errorCode) {
		MvcTestResult result = request.exchange();
		String requestId = result.getResponse().getHeader("X-Request-Id");

		assertThat(requestId).isNotBlank();
		assertThat(result).hasStatus(status).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo(errorCode);
		assertThat(result).bodyJson().extractingPath("$.requestId").isEqualTo(requestId);
	}

	@RestController
	static class FailingController {

		@GetMapping("/api/test/boom")
		String boom() {
			throw new IllegalStateException(SECRET);
		}

	}

}
