package io.github.elmatty18.workflowinbox.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTests {

	private static final String APPLY_TRANSITION = "$.paths['/api/v1/actions/{id}/transitions'].post";

	@Autowired
	private MockMvcTester mvc;

	@Test
	void documentsTheUserHeaderOnEveryOperationAndHidesTheResolvedUser() {
		MvcTestResult docs = apiDocs();

		assertThat(docs).bodyJson()
			.extractingPath("$.paths.*.*.parameters[?(@.name == 'X-User-Id')].required")
			.asArray()
			.hasSize(8)
			.containsOnly(true);
		assertThat(docs).bodyJson()
			.extractingPath("$.paths.*.*.parameters[?(@.name == 'X-User-Id')].in")
			.asArray()
			.containsOnly("header");
		assertThat(docs).bodyJson().extractingPath("$.paths.*.*.parameters[?(@.name == 'user')]").asArray().isEmpty();
	}

	@Test
	void documentsErrorResponsesAsProblemDetails() {
		MvcTestResult docs = apiDocs();

		assertThat(docs).bodyJson()
			.extractingPath(APPLY_TRANSITION + ".responses.*.$ref")
			.asArray()
			.contains("#/components/responses/BadRequest", "#/components/responses/Unauthorized",
					"#/components/responses/Forbidden", "#/components/responses/NotFound",
					"#/components/responses/Conflict", "#/components/responses/UnprocessableContent",
					"#/components/responses/InternalError");
		assertThat(docs).bodyJson()
			.extractingPath("$.components.responses.NotFound.content['application/problem+json'].schema.$ref")
			.isEqualTo("#/components/schemas/Problem");
		assertThat(docs).bodyJson()
			.extractingPath("$.components.schemas.Problem.properties")
			.asMap()
			.containsKeys("errorCode", "requestId", "fieldErrors", "detail", "status");
	}

	@Test
	void keepsTheSuccessResponseBody() {
		assertThat(apiDocs()).bodyJson()
			.extractingPath("$.paths['/api/v1/actions'].post.responses['201'].content.*.schema.$ref")
			.asArray()
			.containsOnly("#/components/schemas/ActionResponse");
	}

	@Test
	void documentsOnlyTheApplicationApi() {
		assertThat(apiDocs()).bodyJson()
			.extractingPath("$.paths")
			.asMap()
			.allSatisfy((path, item) -> assertThat(path).startsWith("/api/v1/"));
	}

	@Test
	void swaggerUiIsServedWithoutIdentity() {
		assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
	}

	private MvcTestResult apiDocs() {
		MvcTestResult docs = mvc.get().uri("/v3/api-docs").exchange();
		assertThat(docs).hasStatusOk();
		return docs;
	}

}
