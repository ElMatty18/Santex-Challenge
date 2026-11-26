package io.github.elmatty18.workflowinbox.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The API answers in English whatever the JVM/OS locale and whatever the client asks for.
 * <p>
 * Each request is built the way Tomcat would build it on a Spanish machine for a Spanish client
 * (MockMvc otherwise defaults every request to English, which would hide the problem).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiLanguageIntegrationTests {

	private static final Locale SPANISH = Locale.forLanguageTag("es-AR");

	@Autowired
	private MockMvcTester mvc;

	private Locale originalDefault;

	@BeforeEach
	void useSpanishJvm() {
		originalDefault = Locale.getDefault();
		Locale.setDefault(SPANISH);
	}

	@AfterEach
	void restoreJvmLocale() {
		Locale.setDefault(originalDefault);
	}

	@Test
	void bodyValidationMessagesAreInEnglish() {
		MvcTestResult result = mvc.post()
			.uri("/api/v1/actions")
			.header("X-User-Id", "ana")
			.header("Accept-Language", "es-AR,es;q=0.9")
			.locale(SPANISH)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"type": "ONBOARDING", "title": ""}""")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.fieldErrors[0].message").isEqualTo("must not be blank");
	}

	@Test
	void parameterValidationMessagesAreInEnglish() {
		MvcTestResult result = mvc.get()
			.uri("/api/v1/actions?size=0")
			.header("X-User-Id", "ana")
			.header("Accept-Language", "es-AR,es;q=0.9")
			.locale(SPANISH)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson()
			.extractingPath("$.fieldErrors[0].message")
			.isEqualTo("must be greater than or equal to 1");
	}

}
