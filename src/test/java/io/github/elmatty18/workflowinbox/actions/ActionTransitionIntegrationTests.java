package io.github.elmatty18.workflowinbox.actions;

import static io.github.elmatty18.workflowinbox.actions.ActionsApi.ISO_UTC;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.elmatty18.workflowinbox.shared.events.ActionChangedEvent;
import io.github.elmatty18.workflowinbox.shared.events.FieldChange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Every test creates its own actions; seeded data is never modified.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class ActionTransitionIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ApplicationEvents events;

	private ActionsApi api;

	@BeforeEach
	void setUp() {
		api = new ActionsApi(mvc);
	}

	@Test
	void assignedReviewerCompletes() {
		long id = api.createAction("ana", ActionType.EXPENSE_APPROVAL, "lucia");
		events.clear();

		MvcTestResult result = api.transition("lucia", id, "COMPLETE", null, "APPROVED", "Within policy");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.state.name").isEqualTo("COMPLETED");
		assertThat(result).bodyJson().extractingPath("$.outcome").isEqualTo("APPROVED");
		assertThat(result).bodyJson().extractingPath("$.comment").isEqualTo("Within policy");
		assertThat(result).bodyJson().extractingPath("$.completedBy").isEqualTo("lucia");
		assertThat(result).bodyJson().extractingPath("$.completedAt").asString().matches(ISO_UTC);
		assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(2);

		assertThat(events.stream(ActionChangedEvent.class)).singleElement().satisfies(event -> {
			assertThat(event.userId()).isEqualTo("lucia");
			assertThat(event.changes()).containsExactly(new FieldChange("state", "PENDING", "COMPLETED"),
					new FieldChange("outcome", null, "APPROVED"), new FieldChange("comment", null, "Within policy"));
		});
	}

	@Test
	void reviewerNotAssignedGets404() {
		long id = api.createAction("ana", ActionType.EXPENSE_APPROVAL, "lucia");

		assertError(api.transition("martin", id, "COMPLETE", null, "APPROVED", null), HttpStatus.NOT_FOUND,
				"ACTION_NOT_FOUND");
	}

	@Test
	void requesterCannotAssignSomeoneElsesAction() {
		long id = api.createAction("admin", ActionType.ONBOARDING, null);

		assertError(api.transition("ana", id, "ASSIGN", "lucia", null, null), HttpStatus.NOT_FOUND, "ACTION_NOT_FOUND");
	}

	@Test
	void requesterCannotComplete() {
		long id = api.createAction("ana", ActionType.ONBOARDING, "lucia");

		assertError(api.transition("ana", id, "COMPLETE", null, "DONE", null), HttpStatus.FORBIDDEN, "FORBIDDEN");
	}

	@Test
	void transitionNotDefinedFromCurrentStateIs409() {
		long id = api.createAction("ana", ActionType.ONBOARDING, null);

		assertError(api.transition("admin", id, "COMPLETE", null, "DONE", null), HttpStatus.CONFLICT,
				"INVALID_TRANSITION");
	}

	@Test
	void outcomeInvalidForTypeIs422() {
		long id = api.createAction("ana", ActionType.DOCUMENT_UPLOAD, "lucia");

		assertError(api.transition("lucia", id, "COMPLETE", null, "APPROVED", null), HttpStatus.UNPROCESSABLE_CONTENT,
				"BUSINESS_RULE_VIOLATION");
	}

	@Test
	void deploymentReviewAlwaysNeedsComment() {
		long id = api.createAction("ana", ActionType.DEPLOYMENT_REVIEW, "lucia");

		assertError(api.transition("lucia", id, "COMPLETE", null, "APPROVED", "  "), HttpStatus.UNPROCESSABLE_CONTENT,
				"BUSINESS_RULE_VIOLATION");
	}

	@Test
	void rejectedExpenseNeedsComment() {
		long id = api.createAction("ana", ActionType.EXPENSE_APPROVAL, "lucia");

		assertError(api.transition("lucia", id, "COMPLETE", null, "REJECTED", null), HttpStatus.UNPROCESSABLE_CONTENT,
				"BUSINESS_RULE_VIOLATION");
	}

	@Test
	void adminReopensKeepingAssigneeAndStoringReason() {
		long id = completedExpense();
		events.clear();

		MvcTestResult result = api.transition("admin", id, "REOPEN", null, null, "Receipt was missing");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.state.name").isEqualTo("PENDING");
		assertThat(result).bodyJson().extractingPath("$.assigneeId").isEqualTo("lucia");
		assertThat(result).bodyJson().extractingPath("$.comment").isEqualTo("Receipt was missing");
		assertThat(result).bodyJson().extractingPath("$.outcome").isNull();
		assertThat(result).bodyJson().extractingPath("$.completedBy").isNull();
		assertThat(result).bodyJson().extractingPath("$.completedAt").isNull();
		assertThat(events.stream(ActionChangedEvent.class)).singleElement()
			.satisfies(event -> assertThat(event.changes()).containsExactly(
					new FieldChange("state", "COMPLETED", "PENDING"), new FieldChange("outcome", "APPROVED", null),
					new FieldChange("comment", "Within policy", "Receipt was missing")));
	}

	@Test
	void reopenRequiresReason() {
		long id = completedExpense();

		assertError(api.transition("admin", id, "REOPEN", null, null, null), HttpStatus.UNPROCESSABLE_CONTENT,
				"BUSINESS_RULE_VIOLATION");
	}

	@Test
	void reviewerCannotReopen() {
		long id = completedExpense();

		assertError(api.transition("lucia", id, "REOPEN", null, null, "I changed my mind"), HttpStatus.FORBIDDEN,
				"FORBIDDEN");
	}

	@Test
	void adminReassignsAndPreviousReviewerLosesVisibility() {
		long id = api.createAction("ana", ActionType.ONBOARDING, "lucia");

		MvcTestResult result = api.transition("admin", id, "REASSIGN", "martin", null, null);

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.assigneeId").isEqualTo("martin");
		assertThat(api.get("lucia", "/api/v1/actions/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(api.get("martin", "/api/v1/actions/{id}", id)).hasStatusOk();
	}

	@Test
	void reassignToCurrentReviewerIs422() {
		long id = api.createAction("ana", ActionType.ONBOARDING, "lucia");

		assertError(api.transition("admin", id, "REASSIGN", "lucia", null, null), HttpStatus.UNPROCESSABLE_CONTENT,
				"BUSINESS_RULE_VIOLATION");
	}

	@Test
	void completingAgainWithSameOutcomeChangesNothing() {
		long id = completedExpense();
		MvcTestResult first = api.get("lucia", "/api/v1/actions/{id}", id);
		events.clear();

		MvcTestResult repeat = api.transition("lucia", id, "COMPLETE", null, "APPROVED", "Within policy");

		assertThat(repeat).hasStatusOk();
		assertThat(ActionsApi.read(repeat, "$.version")).isEqualTo(ActionsApi.read(first, "$.version"));
		assertThat(ActionsApi.read(repeat, "$.updatedAt")).isEqualTo(ActionsApi.read(first, "$.updatedAt"));
		assertThat(events.stream(ActionChangedEvent.class)).isEmpty();
	}

	@Test
	void completingAgainWithDifferentOutcomeIs409WithWhoAndWhen() {
		long id = completedExpense();
		Object completedAt = ActionsApi.read(api.get("lucia", "/api/v1/actions/{id}", id), "$.completedAt");

		MvcTestResult result = api.transition("admin", id, "COMPLETE", null, "REJECTED", "Over budget");

		assertError(result, HttpStatus.CONFLICT, "ACTION_ALREADY_COMPLETED");
		assertThat(result).bodyJson().extractingPath("$.outcome").isEqualTo("APPROVED");
		assertThat(result).bodyJson().extractingPath("$.completedBy").isEqualTo("lucia");
		assertThat(result).bodyJson().extractingPath("$.completedAt").asString().matches(ISO_UTC);
		assertThat(result).bodyJson().extractingPath("$.completedAt").isEqualTo(completedAt);
	}

	@Test
	void rejectsMissingTransitionCode() {
		long id = api.createAction("ana", ActionType.ONBOARDING, null);

		MvcTestResult result = api.transition("ana", id, "{}");

		assertError(result, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
		assertThat(result).bodyJson().extractingPath("$.fieldErrors[*].field").asArray().containsExactly("transition");
	}

	@Test
	void rejectsUnknownOutcome() {
		long id = api.createAction("ana", ActionType.ONBOARDING, "lucia");

		MvcTestResult result = api.transition("lucia", id, "COMPLETE", null, "MAYBE", null);

		assertError(result, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
		assertThat(result).bodyJson().extractingPath("$.fieldErrors[*].field").asArray().containsExactly("outcome");
	}

	/**
	 * @return a new EXPENSE_APPROVAL completed by lucia as APPROVED with comment "Within policy"
	 */
	private long completedExpense() {
		long id = api.createAction("ana", ActionType.EXPENSE_APPROVAL, "lucia");
		assertThat(api.transition("lucia", id, "COMPLETE", null, "APPROVED", "Within policy")).hasStatusOk();
		return id;
	}

	private static void assertError(MvcTestResult result, HttpStatus status, String errorCode) {
		assertThat(result).hasStatus(status).bodyJson().extractingPath("$.errorCode").isEqualTo(errorCode);
	}

}
