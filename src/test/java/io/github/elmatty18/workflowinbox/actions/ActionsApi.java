package io.github.elmatty18.workflowinbox.actions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

import com.jayway.jsonpath.JsonPath;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Thin HTTP client over MockMvc for the actions API, so tests read as API calls.
 */
final class ActionsApi {

	static final String ISO_UTC = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$";

	private final MockMvcTester mvc;

	ActionsApi(MockMvcTester mvc) {
		this.mvc = mvc;
	}

	MvcTestResult create(String userId, String body) {
		return mvc.post()
			.uri("/api/v1/actions")
			.header("X-User-Id", userId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	/**
	 * Creates an action through the API and returns its id.
	 */
	long createAction(String userId, ActionType type, String assigneeId) {
		MvcTestResult result = create(userId, createBody(type, "Test " + type, assigneeId));
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return ((Number) read(result, "$.id")).longValue();
	}

	MvcTestResult transition(String userId, long actionId, String transition, String assigneeId, String outcome,
			String comment) {
		String body = """
				{"transition": %s, "assigneeId": %s, "outcome": %s, "comment": %s}"""
			.formatted(json(transition), json(assigneeId), json(outcome), json(comment));
		return transition(userId, actionId, body);
	}

	MvcTestResult transition(String userId, long actionId, String body) {
		return mvc.post()
			.uri("/api/v1/actions/{id}/transitions", actionId)
			.header("X-User-Id", userId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	MvcTestResult get(String userId, String uri, Object... uriVariables) {
		return mvc.get().uri(uri, uriVariables).header("X-User-Id", userId).exchange();
	}

	static String createBody(ActionType type, String title, String assigneeId) {
		return """
				{"type": %s, "title": %s, "description": "Created by a test", "assigneeId": %s}"""
			.formatted(json(type == null ? null : type.name()), json(title), json(assigneeId));
	}

	static Object read(MvcTestResult result, String jsonPath) {
		try {
			return JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static String json(String value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

}
