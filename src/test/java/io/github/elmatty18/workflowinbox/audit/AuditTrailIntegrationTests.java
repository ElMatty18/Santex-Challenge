package io.github.elmatty18.workflowinbox.audit;

import static io.github.elmatty18.workflowinbox.audit.AuditApi.read;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * How changes reach the audit log. Every test creates its own actions; seeded data is never
 * modified.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditTrailIntegrationTests {

	private static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private AuditApi api;

	@BeforeEach
	void setUp() {
		api = new AuditApi(mvc);
	}

	@Test
	void createWithAssigneeAndCompleteWriteTheRowsOfSection11() {
		String createRequest = "create-" + UUID.randomUUID();
		String completeRequest = "complete-" + UUID.randomUUID();

		MvcTestResult created = api.createAction("ana", "EXPENSE_APPROVAL", "lucia", createRequest);
		long id = ((Number) read(created, "$.id")).longValue();
		MvcTestResult completed = api.transition("lucia", id, """
				{"transition": "COMPLETE", "outcome": "APPROVED", "comment": "Within policy"}""", completeRequest);

		assertThat(created.getResponse().getHeader("X-Request-Id")).isEqualTo(createRequest);
		assertThat(completed).hasStatusOk();
		List<Map<String, Object>> rows = api.rowsOldestFirst(id);
		assertThat(rows).map(AuditApi::describe)
			.containsExactly("ana | state | null | CREATED | " + createRequest,
					"ana | assignee | null | lucia | " + createRequest,
					"ana | state | CREATED | PENDING | " + createRequest,
					"lucia | state | PENDING | COMPLETED | " + completeRequest,
					"lucia | outcome | null | APPROVED | " + completeRequest,
					"lucia | comment | null | Within policy | " + completeRequest);
		assertThat(rows).allSatisfy(row -> assertThat(row.get("actionId")).isEqualTo((int) id));
		// one timestamp per change: the completion rows share the action's completedAt
		assertThat(rows.subList(3, 6)).map(row -> row.get("changedAt"))
			.containsOnly((Object) read(completed, "$.completedAt"));
	}

	@Test
	void generatedRequestIdIsStoredWhenTheCallerSendsNone() {
		MvcTestResult created = api.createAction("ana", "ONBOARDING", null, null);

		String requestId = created.getResponse().getHeader("X-Request-Id");
		assertThat(requestId).matches(UUID_PATTERN);
		long id = ((Number) read(created, "$.id")).longValue();
		assertThat(api.rowsOldestFirst(id)).singleElement()
			.satisfies(row -> assertThat(row.get("requestId")).isEqualTo(requestId));
	}

	@Test
	void rejectedTransitionsWriteNoAuditRows() {
		long id = api.createActionId("ana", "EXPENSE_APPROVAL", "lucia");
		int rowsAfterCreate = api.rowsOldestFirst(id).size();

		// 422 outcome not valid for the type
		assertThat(api.transition("lucia", id, """
				{"transition": "COMPLETE", "outcome": "DONE"}""", null)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		// 409 not defined from PENDING
		assertThat(api.transition("ana", id, """
				{"transition": "ASSIGN", "assigneeId": "martin"}""", null)).hasStatus(HttpStatus.CONFLICT);
		// 403 role not allowed
		assertThat(api.transition("ana", id, """
				{"transition": "COMPLETE", "outcome": "APPROVED"}""", null)).hasStatus(HttpStatus.FORBIDDEN);
		// 404 not visible
		assertThat(api.transition("martin", id, """
				{"transition": "COMPLETE", "outcome": "APPROVED"}""", null)).hasStatus(HttpStatus.NOT_FOUND);

		assertThat(api.rowsOldestFirst(id)).hasSize(rowsAfterCreate);
	}

	@Test
	void idempotentRepeatAndConflictingCompletionWriteNoAuditRows() {
		long id = api.createActionId("ana", "EXPENSE_APPROVAL", "lucia");
		String approve = """
				{"transition": "COMPLETE", "outcome": "APPROVED"}""";
		assertThat(api.transition("lucia", id, approve, null)).hasStatusOk();
		int rowsAfterComplete = api.rowsOldestFirst(id).size();

		assertThat(api.transition("lucia", id, approve, null)).hasStatusOk();
		assertThat(api.transition("admin", id, """
				{"transition": "COMPLETE", "outcome": "REJECTED", "comment": "No"}""", null))
			.hasStatus(HttpStatus.CONFLICT);

		assertThat(api.rowsOldestFirst(id)).hasSize(rowsAfterComplete);
	}

	@Test
	void auditWriteFailureRollsBackTheActionChange() {
		long id = api.createActionId("ana", "ONBOARDING", "lucia");
		List<String> rowsBefore = api.rowsOldestFirst(id).stream().map(AuditApi::describe).toList();
		Object versionBefore = read(getAction(id), "$.version");

		// make the database refuse one specific audit value, so the audit insert itself fails
		jdbc.execute("ALTER TABLE audit_log ADD CONSTRAINT ck_test_audit_failure "
				+ "CHECK (new_value IS NULL OR new_value <> 'FAIL-AUDIT')");
		MvcTestResult result;
		try {
			result = api.transition("lucia", id, """
					{"transition": "COMPLETE", "outcome": "DONE", "comment": "FAIL-AUDIT"}""", null);
		}
		finally {
			jdbc.execute("ALTER TABLE audit_log DROP CONSTRAINT ck_test_audit_failure");
		}

		assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
			.bodyJson()
			.extractingPath("$.errorCode")
			.isEqualTo("INTERNAL_ERROR");
		assertThat(result).bodyJson().extractingPath("$.detail").asString().doesNotContain("FAIL-AUDIT", "constraint");
		MvcTestResult action = getAction(id);
		assertThat(action).bodyJson().extractingPath("$.state.name").isEqualTo("PENDING");
		assertThat(action).bodyJson().extractingPath("$.outcome").isNull();
		assertThat(action).bodyJson().extractingPath("$.comment").isNull();
		assertThat(action).bodyJson().extractingPath("$.completedBy").isNull();
		assertThat(action).bodyJson().extractingPath("$.version").isEqualTo(versionBefore);
		assertThat(api.rowsOldestFirst(id)).map(AuditApi::describe).isEqualTo(rowsBefore);
	}

	private MvcTestResult getAction(long id) {
		return mvc.get().uri("/api/v1/actions/{id}", id).header("X-User-Id", "lucia").exchange();
	}

}
