package io.github.elmatty18.workflowinbox.actions;

import static io.github.elmatty18.workflowinbox.actions.ActionsApi.ISO_UTC;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Read-only tests over the seeded actions (V3/V4), which no test modifies:
 * <ul>
 * <li>1 EXPENSE_APPROVAL, CREATED, requested by ana, created 2026-11-20</li>
 * <li>2 DEPLOYMENT_REVIEW, PENDING, ana, assigned to lucia, 2026-11-21</li>
 * <li>3 DOCUMENT_UPLOAD, PENDING, admin, assigned to martin, 2026-11-22</li>
 * <li>4 ONBOARDING, COMPLETED (DONE by lucia), ana, assigned to lucia, 2026-11-18</li>
 * </ul>
 * Other tests add actions, so lists are checked by containment, never by exact size. Added
 * actions are always newer than the seeded ones, so the seeded ones come first (oldest first).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActionQueryIntegrationTests {

	private static final String INBOX = "/api/v1/actions?size=100";

	@Autowired
	private MockMvcTester mvc;

	private ActionsApi api;

	@BeforeEach
	void setUp() {
		api = new ActionsApi(mvc);
	}

	@Test
	void requesterSeesOnlyOwnActions() {
		MvcTestResult result = api.get("ana", INBOX);

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.items[*].requesterId").asArray().containsOnly("ana");
		assertThat(result).bodyJson().extractingPath("$.items[*].id").asArray().contains(1, 2, 4).doesNotContain(3);
	}

	@Test
	void reviewerSeesOnlyAssignedActions() {
		MvcTestResult result = api.get("lucia", INBOX);

		assertThat(result).bodyJson().extractingPath("$.items[*].assigneeId").asArray().containsOnly("lucia");
		assertThat(result).bodyJson().extractingPath("$.items[*].id").asArray().contains(2, 4).doesNotContain(1, 3);
	}

	@Test
	void adminSeesEverythingOldestFirst() {
		assertThat(api.get("admin", INBOX)).bodyJson()
			.extractingPath("$.items[0:4].id")
			.asArray()
			.containsExactly(4, 1, 2, 3);
	}

	@Test
	void filtersByStateName() {
		MvcTestResult result = api.get("admin", INBOX + "&state=PENDING");

		assertThat(result).bodyJson().extractingPath("$.items[*].state.name").asArray().containsOnly("PENDING");
		assertThat(result).bodyJson().extractingPath("$.items[*].id").asArray().contains(2, 3).doesNotContain(1, 4);
	}

	@Test
	void combinesFilters() {
		MvcTestResult result = api.get("admin", INBOX + "&state=COMPLETED&type=ONBOARDING&assigneeId=lucia");

		assertThat(result).bodyJson().extractingPath("$.items[*].type").asArray().containsOnly("ONBOARDING");
		assertThat(result).bodyJson().extractingPath("$.items[*].id").asArray().contains(4);
	}

	@Test
	void filtersAreCombinedWithVisibility() {
		// ana asks for martin's actions: action 3 is assigned to martin but not hers
		assertThat(api.get("ana", INBOX + "&assigneeId=martin")).bodyJson()
			.extractingPath("$.items[*].id")
			.asArray()
			.doesNotContain(3);
	}

	@Test
	void paginates() {
		MvcTestResult result = api.get("admin", "/api/v1/actions?page=1&size=2");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.items[*].id").asArray().containsExactly(2, 3);
		assertThat(result).bodyJson().extractingPath("$.page").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(2);
		long total = ((Number) ActionsApi.read(result, "$.totalElements")).longValue();
		assertThat(total).isGreaterThanOrEqualTo(4);
		assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo((int) ((total + 1) / 2));
	}

	@Test
	void rejectsUnknownState() {
		assertValidationError(api.get("admin", "/api/v1/actions?state=ARCHIVED"), "state");
	}

	@Test
	void rejectsUnknownType() {
		assertValidationError(api.get("admin", "/api/v1/actions?type=VACATION"), "type");
	}

	@Test
	void rejectsPageSizeOutOfRange() {
		assertValidationError(api.get("admin", "/api/v1/actions?size=101"), "size");
		assertValidationError(api.get("admin", "/api/v1/actions?size=0"), "size");
		assertValidationError(api.get("admin", "/api/v1/actions?page=-1"), "page");
	}

	@Test
	void returnsActionDetailsWithIsoTimestamps() {
		MvcTestResult result = api.get("ana", "/api/v1/actions/{id}", 4);

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.state.code").isEqualTo(100);
		assertThat(result).bodyJson().extractingPath("$.state.name").isEqualTo("COMPLETED");
		assertThat(result).bodyJson().extractingPath("$.outcome").isEqualTo("DONE");
		assertThat(result).bodyJson().extractingPath("$.completedBy").isEqualTo("lucia");
		assertThat(result).bodyJson().extractingPath("$.completedAt").isEqualTo("2026-11-19T15:30:00Z");
		assertThat(result).bodyJson().extractingPath("$.createdAt").isEqualTo("2026-11-18T08:00:00Z");
		assertThat(result).bodyJson().extractingPath("$.updatedAt").asString().matches(ISO_UTC);
	}

	@Test
	void hiddenActionLooksLikeMissingOne() {
		MvcTestResult hidden = api.get("ana", "/api/v1/actions/{id}", 3);
		MvcTestResult missing = api.get("admin", "/api/v1/actions/{id}", 999_999);

		assertThat(hidden).hasStatus(HttpStatus.NOT_FOUND).bodyJson().extractingPath("$.errorCode").isEqualTo("ACTION_NOT_FOUND");
		assertThat(missing).hasStatus(HttpStatus.NOT_FOUND).bodyJson().extractingPath("$.errorCode").isEqualTo("ACTION_NOT_FOUND");
		assertThat(api.get("martin", "/api/v1/actions/{id}", 2)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void rejectsNonNumericId() {
		assertValidationError(api.get("admin", "/api/v1/actions/abc"), "id");
	}

	@Test
	void listsTransitionsAvailableToTheCaller() {
		assertThat(api.get("ana", "/api/v1/actions/{id}/transitions", 1)).bodyJson()
			.isLenientlyEqualTo("""
					[{"code": "ASSIGN", "toStateCode": 10, "toStateName": "PENDING"}]""");
		assertThat(api.get("lucia", "/api/v1/actions/{id}/transitions", 2)).bodyJson()
			.isLenientlyEqualTo("""
					[{"code": "COMPLETE", "toStateCode": 100, "toStateName": "COMPLETED"}]""");
		assertThat(api.get("admin", "/api/v1/actions/{id}/transitions", 4)).bodyJson()
			.isLenientlyEqualTo("""
					[{"code": "REOPEN", "toStateCode": 10, "toStateName": "PENDING"}]""");
		assertThat(api.get("lucia", "/api/v1/actions/{id}/transitions", 4)).bodyJson().isLenientlyEqualTo("[]");
		assertThat(api.get("martin", "/api/v1/actions/{id}/transitions", 2)).hasStatus(HttpStatus.NOT_FOUND);
	}

	private static void assertValidationError(MvcTestResult result, String field) {
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
		assertThat(result).bodyJson().extractingPath("$.fieldErrors[*].field").asArray().containsExactly(field);
	}

}
