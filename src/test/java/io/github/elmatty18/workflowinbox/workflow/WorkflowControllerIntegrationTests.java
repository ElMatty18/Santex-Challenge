package io.github.elmatty18.workflowinbox.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Runs against the real Flyway-seeded H2 database, so it also proves that the seeded workflow
 * passes startup validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WorkflowControllerIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void listsConfiguredStates() {
		var result = assertThat(mvc.get().uri("/api/v1/workflow/states").header("X-User-Id", "ana"));

		result.hasStatusOk();
		result.bodyJson().extractingPath("$[*].name").asArray().containsExactly("CREATED", "PENDING", "COMPLETED");
		result.bodyJson().extractingPath("$[?(@.initial == true)].name").asArray().containsExactly("CREATED");
		result.bodyJson().extractingPath("$[?(@.finalState == true)].name").asArray().containsExactly("COMPLETED");
	}

	@Test
	void listsConfiguredTransitions() {
		var result = assertThat(mvc.get().uri("/api/v1/workflow/transitions").header("X-User-Id", "lucia"));

		result.hasStatusOk();
		result.bodyJson().extractingPath("$.length()").isEqualTo(6);
		result.bodyJson().extractingPath("$[?(@.code == 'REOPEN')].allowedRole").asArray().containsExactly("ADMIN");
	}

	@Test
	void requiresIdentity() {
		assertThat(mvc.get().uri("/api/v1/workflow/states")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

}
