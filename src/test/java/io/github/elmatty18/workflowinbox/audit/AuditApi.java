package io.github.elmatty18.workflowinbox.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * HTTP helpers for audit tests. Actions are created and changed through the public API only.
 */
final class AuditApi {

	private final MockMvcTester mvc;

	AuditApi(MockMvcTester mvc) {
		this.mvc = mvc;
	}

	MvcTestResult createAction(String userId, String type, String assigneeId, String requestId) {
		String body = """
				{"type": "%s", "title": "Audit test", "assigneeId": %s}"""
			.formatted(type, assigneeId == null ? "null" : "\"" + assigneeId + "\"");
		return mvc.perform(withRequestId(post("/api/v1/actions", userId, body), requestId));
	}

	long createActionId(String userId, String type, String assigneeId) {
		MvcTestResult result = createAction(userId, type, assigneeId, null);
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return ((Number) read(result, "$.id")).longValue();
	}

	MvcTestResult transition(String userId, long actionId, String body, String requestId) {
		return mvc.perform(withRequestId(post("/api/v1/actions/" + actionId + "/transitions", userId, body), requestId));
	}

	MvcTestResult search(String userId, String query) {
		return mvc.get().uri("/api/v1/audit?" + query).header("X-User-Id", userId).exchange();
	}

	/**
	 * Audit rows of one action, oldest first (the API returns newest first).
	 */
	List<Map<String, Object>> rowsOldestFirst(long actionId) {
		List<Map<String, Object>> newestFirst = read(search("admin", "size=100&actionId=" + actionId), "$.items");
		return newestFirst.reversed();
	}

	@SuppressWarnings("unchecked")
	static <T> T read(MvcTestResult result, String jsonPath) {
		try {
			return (T) JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * A row as "userId | field | oldValue | newValue | requestId", for readable comparisons.
	 */
	static String describe(Map<String, Object> row) {
		return "%s | %s | %s | %s | %s".formatted(row.get("userId"), row.get("field"), row.get("oldValue"),
				row.get("newValue"), row.get("requestId"));
	}

	private MockHttpServletRequestBuilder post(String uri, String userId, String body) {
		return MockMvcRequestBuilders.post(uri)
			.header("X-User-Id", userId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body);
	}

	private static MockHttpServletRequestBuilder withRequestId(MockHttpServletRequestBuilder request, String requestId) {
		return requestId == null ? request : request.header("X-Request-Id", requestId);
	}

}
