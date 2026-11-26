package io.github.elmatty18.workflowinbox.audit;

import static io.github.elmatty18.workflowinbox.audit.AuditApi.read;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Read-only tests over the seeded audit rows (V3), all dated 2026-11-18 to 2026-11-22. Rows
 * written by other tests are dated "now", so every query here is bounded to the seed's dates or
 * to a seeded action.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditSearchIntegrationTests {

	private static final String SEED_PERIOD = "from=2026-11-18T00:00:00Z&to=2026-11-23T00:00:00Z";

	@Autowired
	private MockMvcTester mvc;

	private AuditApi api;

	@BeforeEach
	void setUp() {
		api = new AuditApi(mvc);
	}

	@Test
	void searchesByActionNewestFirst() {
		MvcTestResult result = api.search("admin", "actionId=4");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.items[*].field").asArray()
			.containsExactly("comment", "outcome", "state", "state", "assignee", "state");
		assertThat(result).bodyJson().extractingPath("$.items[0].changedAt").isEqualTo("2026-11-19T15:30:00Z");
		assertThat(result).bodyJson().extractingPath("$.items[5].changedAt").isEqualTo("2026-11-18T08:00:00Z");
		assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(6);
	}

	@Test
	void searchesByUser() {
		MvcTestResult result = api.search("admin", "userId=admin&" + SEED_PERIOD);

		assertThat(result).bodyJson().extractingPath("$.items[*].userId").asArray().containsOnly("admin");
		assertThat(result).bodyJson().extractingPath("$.items[*].actionId").asArray().containsExactly(3, 3, 3);
	}

	@Test
	void searchesByInclusiveDateRange() {
		MvcTestResult result = api.search("admin", "from=2026-11-19T15:30:00Z&to=2026-11-21T10:00:00Z");

		// both bounds are hit exactly: action 4's completion and action 2's creation
		assertThat(result).bodyJson().extractingPath("$.items[*].actionId").asArray()
			.containsExactly(2, 2, 2, 1, 4, 4, 4);
		assertThat(result).bodyJson().extractingPath("$.items[0].changedAt").isEqualTo("2026-11-21T10:00:00Z");
		assertThat(result).bodyJson().extractingPath("$.items[6].changedAt").isEqualTo("2026-11-19T15:30:00Z");
	}

	@Test
	void acceptsInstantsWithOffset() {
		// 07:00-03:00 is 10:00Z: only action 2's creation
		assertThat(api.search("admin", "from=2026-11-21T07:00:00-03:00&to=2026-11-21T07:00:00-03:00"))
			.bodyJson()
			.extractingPath("$.items[*].actionId")
			.asArray()
			.containsExactly(2, 2, 2);
	}

	@Test
	void combinesFilters() {
		MvcTestResult result = api.search("admin", "userId=lucia&actionId=4&" + SEED_PERIOD);

		assertThat(result).bodyJson().extractingPath("$.items[*].field").asArray()
			.containsExactly("comment", "outcome", "state");
		assertThat(result).bodyJson().extractingPath("$.items[*].userId").asArray().containsOnly("lucia");
	}

	@Test
	void paginates() {
		MvcTestResult result = api.search("admin", "actionId=4&size=4&page=1");

		assertThat(result).bodyJson().extractingPath("$.items[*].field").asArray().containsExactly("assignee", "state");
		assertThat(result).bodyJson().extractingPath("$.page").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(4);
		assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(6);
		assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(2);
	}

	@Test
	void rejectsFromAfterTo() {
		assertValidationError(api.search("admin", "from=2026-11-22T00:00:00Z&to=2026-11-21T00:00:00Z"), "from");
	}

	@Test
	void rejectsMalformedDate() {
		assertValidationError(api.search("admin", "from=yesterday"), "from");
	}

	@Test
	void rejectsPageSizeOutOfRange() {
		assertValidationError(api.search("admin", "size=101"), "size");
	}

	@ParameterizedTest
	@ValueSource(strings = { "ana", "lucia" })
	void onlyAdminMaySearch(String userId) {
		assertThat(api.search(userId, "actionId=4")).hasStatus(HttpStatus.FORBIDDEN)
			.bodyJson()
			.extractingPath("$.errorCode")
			.isEqualTo("FORBIDDEN");
	}

	@Test
	void requiresIdentity() {
		assertThat(mvc.get().uri("/api/v1/audit")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Replaying each seeded action's audit rows in order must reproduce its current fields: every
	 * old value matches the previous new value, and the last new value matches the action.
	 */
	@ParameterizedTest
	@ValueSource(longs = { 1, 2, 3, 4 })
	void seededAuditRowsAreConsistentWithSeededActions(long actionId) {
		List<Map<String, Object>> rows = api.rowsOldestFirst(actionId);
		MvcTestResult action = mvc.get().uri("/api/v1/actions/{id}", actionId).header("X-User-Id", "admin").exchange();

		assertThat(rows).first().satisfies(row -> {
			assertThat(row.get("field")).isEqualTo("state");
			assertThat(row.get("oldValue")).isNull();
			assertThat(row.get("newValue")).isEqualTo("CREATED");
		});
		assertThat(rows).allSatisfy(row -> assertThat(row.get("requestId")).isEqualTo("seed"));

		Map<String, Object> replayed = new HashMap<>();
		for (Map<String, Object> row : rows) {
			String field = (String) row.get("field");
			assertThat(row.get("oldValue")).as("old value of %s", field).isEqualTo(replayed.get(field));
			replayed.put(field, row.get("newValue"));
		}
		assertThat(replayed.get("state")).isEqualTo(read(action, "$.state.name"));
		assertThat(replayed.get("assignee")).isEqualTo(read(action, "$.assigneeId"));
		assertThat(replayed.get("outcome")).isEqualTo(read(action, "$.outcome"));
		assertThat(replayed.get("comment")).isEqualTo(read(action, "$.comment"));
		assertThat(rows).last().satisfies(row -> {
			Object completedAt = read(action, "$.completedAt");
			if (completedAt != null) {
				assertThat(row.get("changedAt")).isEqualTo(completedAt);
				assertThat(row.get("userId")).isEqualTo(read(action, "$.completedBy"));
			}
			else {
				assertThat(row.get("changedAt")).isEqualTo(read(action, "$.createdAt"));
			}
		});
	}

	private static void assertValidationError(MvcTestResult result, String field) {
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
		assertThat(result).bodyJson().extractingPath("$.fieldErrors[*].field").asArray().containsExactly(field);
	}

}
