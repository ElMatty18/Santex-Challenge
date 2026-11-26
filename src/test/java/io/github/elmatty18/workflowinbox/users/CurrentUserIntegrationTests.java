package io.github.elmatty18.workflowinbox.users;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CurrentUserIntegrationTests.WhoAmIController.class)
class CurrentUserIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void missingUserHeaderReturns401() {
		assertThat(mvc.get().uri("/api/test/whoami"))
			.hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.errorCode")
			.isEqualTo("UNAUTHENTICATED");
	}

	@Test
	void unknownUserReturns401() {
		assertThat(mvc.get().uri("/api/test/whoami").header("X-User-Id", "nobody"))
			.hasStatus(HttpStatus.UNAUTHORIZED)
			.bodyJson()
			.extractingPath("$.errorCode")
			.isEqualTo("UNAUTHENTICATED");
	}

	@Test
	void knownUserIsResolved() {
		var result = assertThat(mvc.get().uri("/api/test/whoami").header("X-User-Id", "lucia"));

		result.hasStatusOk();
		result.bodyJson().extractingPath("$.id").isEqualTo("lucia");
		result.bodyJson().extractingPath("$.role").isEqualTo("REVIEWER");
	}

	@Test
	void actuatorHealthDoesNotRequireUserHeader() {
		assertThat(mvc.get().uri("/actuator/health"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("UP");
	}

	@RestController
	static class WhoAmIController {

		@GetMapping("/api/test/whoami")
		User whoAmI(@CurrentUser User user) {
			return user;
		}

	}

}
