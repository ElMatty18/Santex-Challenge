package io.github.elmatty18.workflowinbox.workflow;

import static io.github.elmatty18.workflowinbox.workflow.TestWorkflows.ADMIN_USER;
import static io.github.elmatty18.workflowinbox.workflow.TestWorkflows.ANA;
import static io.github.elmatty18.workflowinbox.workflow.TestWorkflows.COMPLETED;
import static io.github.elmatty18.workflowinbox.workflow.TestWorkflows.LUCIA;
import static io.github.elmatty18.workflowinbox.workflow.TestWorkflows.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;
import io.github.elmatty18.workflowinbox.users.User;
import io.github.elmatty18.workflowinbox.users.UserDirectory;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TransitionEngineTests {

	private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");

	private static final Instant EARLIER = Instant.parse("2026-11-25T09:30:00Z");

	private final UserDirectory directory = id -> TestWorkflows.USERS.stream().filter(u -> u.id().equals(id)).findFirst();

	private final TransitionEngine engine = engine(TestWorkflows.seeded());

	private TransitionEngine engine(WorkflowDefinition definition) {
		return new TransitionEngine(definition,
				new TransitionHandlerRegistry(List.of(new AssignTransitionHandler(directory),
						new CompleteTransitionHandler(Clock.fixed(NOW, ZoneOffset.UTC)), new ReopenTransitionHandler())));
	}

	@Nested
	class AvailableTransitions {

		@Test
		void requesterCanAssignCreatedItem() {
			assertThat(engine.availableTransitions(FakeWorkflowItem.created(), ANA))
				.containsExactly(new AvailableTransition("ASSIGN", PENDING, "PENDING"));
		}

		@Test
		void reviewerCanCompletePendingItem() {
			assertThat(engine.availableTransitions(FakeWorkflowItem.pendingFor("lucia"), LUCIA))
				.containsExactly(new AvailableTransition("COMPLETE", COMPLETED, "COMPLETED"));
		}

		@Test
		void adminCanReassignOrCompletePendingItem() {
			assertThat(engine.availableTransitions(FakeWorkflowItem.pendingFor("lucia"), ADMIN_USER)).containsExactly(
					new AvailableTransition("REASSIGN", PENDING, "PENDING"),
					new AvailableTransition("COMPLETE", COMPLETED, "COMPLETED"));
		}

		@Test
		void onlyAdminCanReopenCompletedItem() {
			FakeWorkflowItem item = FakeWorkflowItem.completedBy("lucia", Outcome.APPROVED, null, EARLIER);

			assertThat(engine.availableTransitions(item, ADMIN_USER))
				.containsExactly(new AvailableTransition("REOPEN", PENDING, "PENDING"));
			assertThat(engine.availableTransitions(item, LUCIA)).isEmpty();
		}

	}

	@Nested
	class Assign {

		@Test
		void assignsReviewerAndMovesToPending() {
			FakeWorkflowItem item = FakeWorkflowItem.created();

			TransitionResult result = engine.apply(item, ANA, assign("ASSIGN", "lucia"));

			assertThat(result.applied()).isTrue();
			assertThat(result.from().name()).isEqualTo("CREATED");
			assertThat(result.to().name()).isEqualTo("PENDING");
			assertThat(item.stateCode()).isEqualTo(PENDING);
			assertThat(item.assigneeId()).isEqualTo("lucia");
		}

		@Test
		void requiresAssignee() {
			assertRejected(FakeWorkflowItem.created(), ANA, assign("ASSIGN", " "), ErrorCode.BUSINESS_RULE_VIOLATION,
					"assigneeId is required for ASSIGN");
		}

		@Test
		void rejectsUnknownAssignee() {
			assertRejected(FakeWorkflowItem.created(), ANA, assign("ASSIGN", "nobody"),
					ErrorCode.BUSINESS_RULE_VIOLATION, "Assignee 'nobody' must be an existing user with role REVIEWER");
		}

		@Test
		void rejectsAssigneeWhoIsNotReviewer() {
			assertRejected(FakeWorkflowItem.created(), ANA, assign("ASSIGN", "admin"),
					ErrorCode.BUSINESS_RULE_VIOLATION, "Assignee 'admin' must be an existing user with role REVIEWER");
		}

		@Test
		void adminReassignsToAnotherReviewer() {
			FakeWorkflowItem item = FakeWorkflowItem.pendingFor("lucia");

			TransitionResult result = engine.apply(item, ADMIN_USER, assign("REASSIGN", "martin"));

			assertThat(result.to().name()).isEqualTo("PENDING");
			assertThat(item.assigneeId()).isEqualTo("martin");
		}

		@Test
		void rejectsReassignToCurrentAssignee() {
			assertRejected(FakeWorkflowItem.pendingFor("lucia"), ADMIN_USER, assign("REASSIGN", "lucia"),
					ErrorCode.BUSINESS_RULE_VIOLATION, "Action is already assigned to 'lucia'");
		}

		@Test
		void ignoresFieldsAssignDoesNotUse() {
			FakeWorkflowItem item = FakeWorkflowItem.created();

			engine.apply(item, ANA, new TransitionRequest("ASSIGN", "lucia", Outcome.APPROVED, "ignored"));

			assertThat(item.outcome()).isNull();
			assertThat(item.comment()).isNull();
		}

	}

	@Nested
	class TableRules {

		@Test
		void unknownCodeFromCurrentStateIsInvalid() {
			assertRejected(FakeWorkflowItem.created(), ADMIN_USER, complete(Outcome.APPROVED, null),
					ErrorCode.INVALID_TRANSITION, "Transition 'COMPLETE' is not allowed from state CREATED");
		}

		@Test
		void undefinedCodeIsInvalid() {
			assertRejected(FakeWorkflowItem.pendingFor("lucia"), ADMIN_USER, new TransitionRequest("ESCALATE", null, null, null),
					ErrorCode.INVALID_TRANSITION, "Transition 'ESCALATE' is not allowed from state PENDING");
		}

		@Test
		void reviewerCannotAssign() {
			assertRejected(FakeWorkflowItem.created(), LUCIA, assign("ASSIGN", "lucia"), ErrorCode.FORBIDDEN,
					"Role REVIEWER may not apply transition 'ASSIGN' from state CREATED");
		}

		@Test
		void reviewerCannotReopen() {
			assertRejected(FakeWorkflowItem.completedBy("lucia", Outcome.APPROVED, null, EARLIER), LUCIA,
					reopen("Wrong decision"), ErrorCode.FORBIDDEN,
					"Role REVIEWER may not apply transition 'REOPEN' from state COMPLETED");
		}

		@Test
		void codeWithoutHandlerOnlyChangesState() {
			TransitionEngine engine = engine(TestWorkflows.seededWithInReview());
			FakeWorkflowItem item = FakeWorkflowItem.pendingFor("lucia");

			TransitionResult result = engine.apply(item, LUCIA,
					new TransitionRequest("START_REVIEW", "martin", Outcome.DONE, "ignored"));

			assertThat(result.to().name()).isEqualTo("IN_REVIEW");
			assertThat(item.stateCode()).isEqualTo(50);
			assertThat(item.assigneeId()).isEqualTo("lucia");
			assertThat(item.outcome()).isNull();
			assertThat(item.comment()).isNull();
		}

	}

	@Nested
	class Complete {

		@Test
		void completesWithOutcomeCommentAndCompletionData() {
			FakeWorkflowItem item = FakeWorkflowItem.pendingFor("lucia");

			TransitionResult result = engine.apply(item, LUCIA, complete(Outcome.REJECTED, "Over budget"));

			assertThat(result.applied()).isTrue();
			assertThat(item.stateCode()).isEqualTo(COMPLETED);
			assertThat(item.outcome()).isEqualTo(Outcome.REJECTED);
			assertThat(item.comment()).isEqualTo("Over budget");
			assertThat(item.completedBy()).isEqualTo("lucia");
			assertThat(item.completedAt()).isEqualTo(NOW);
		}

		@Test
		void requiresOutcome() {
			assertRejected(FakeWorkflowItem.pendingFor("lucia"), LUCIA, complete(null, null),
					ErrorCode.BUSINESS_RULE_VIOLATION, "outcome is required for COMPLETE");
		}

		@Test
		void appliesCompletionPolicyBeforeChangingAnything() {
			assertRejected(FakeWorkflowItem.pendingFor("lucia"), LUCIA, complete(Outcome.DONE, null),
					ErrorCode.BUSINESS_RULE_VIOLATION, "Outcome DONE is not valid");
			assertRejected(FakeWorkflowItem.pendingFor("lucia"), LUCIA, complete(Outcome.REJECTED, "  "),
					ErrorCode.BUSINESS_RULE_VIOLATION, "comment is required when REJECTED");
		}

	}

	@Nested
	class Reopen {

		@Test
		void storesReasonClearsOutcomeAndKeepsAssignee() {
			FakeWorkflowItem item = FakeWorkflowItem.completedBy("lucia", Outcome.APPROVED, "Within policy", EARLIER);

			TransitionResult result = engine.apply(item, ADMIN_USER, reopen("Receipt was missing"));

			assertThat(result.from().name()).isEqualTo("COMPLETED");
			assertThat(result.to().name()).isEqualTo("PENDING");
			assertThat(item.comment()).isEqualTo("Receipt was missing");
			assertThat(item.outcome()).isNull();
			assertThat(item.completedBy()).isNull();
			assertThat(item.completedAt()).isNull();
			assertThat(item.assigneeId()).isEqualTo("lucia");
		}

		@Test
		void requiresReason() {
			FakeWorkflowItem item = FakeWorkflowItem.completedBy("lucia", Outcome.APPROVED, null, EARLIER);

			assertRejected(item, ADMIN_USER, reopen(null), ErrorCode.BUSINESS_RULE_VIOLATION,
					"comment (the reason) is required for REOPEN");
			assertRejected(item, ADMIN_USER, reopen("   "), ErrorCode.BUSINESS_RULE_VIOLATION,
					"comment (the reason) is required for REOPEN");
		}

	}

	@Nested
	class IdempotentCompletion {

		private final FakeWorkflowItem completed = FakeWorkflowItem.completedBy("lucia", Outcome.APPROVED,
				"Within policy", EARLIER);

		@Test
		void sameOutcomeIsANoOp() {
			FakeWorkflowItem before = completed.copy();

			TransitionResult result = engine.apply(completed, LUCIA, complete(Outcome.APPROVED, "different comment"));

			assertThat(result.applied()).isFalse();
			assertThat(result.from().name()).isEqualTo("COMPLETED");
			assertThat(result.to().name()).isEqualTo("COMPLETED");
			assertThat(completed).usingRecursiveComparison().isEqualTo(before);
		}

		@Test
		void differentOutcomeConflictsWithWhoAndWhen() {
			assertThatThrownBy(() -> engine.apply(completed, ADMIN_USER, complete(Outcome.REJECTED, "Over budget")))
				.isInstanceOfSatisfying(ApiException.class, ex -> {
					assertThat(ex.errorCode()).isEqualTo(ErrorCode.ACTION_ALREADY_COMPLETED);
					assertThat(ex.properties()).containsEntry("outcome", Outcome.APPROVED)
						.containsEntry("completedBy", "lucia")
						.containsEntry("completedAt", EARLIER);
				});
		}

		@Test
		void roleIsCheckedBeforeOutcome() {
			assertRejected(completed, ANA, complete(Outcome.DONE, null), ErrorCode.FORBIDDEN,
					"Role REQUESTER may not apply transition 'COMPLETE' from state COMPLETED");
		}

		@Test
		void outcomeValidityIsCheckedBeforeConflict() {
			assertRejected(completed, LUCIA, complete(Outcome.DONE, null), ErrorCode.BUSINESS_RULE_VIOLATION,
					"Outcome DONE is not valid");
		}

		@Test
		void otherTransitionsAreNotIdempotent() {
			FakeWorkflowItem pending = FakeWorkflowItem.pendingFor("lucia");

			assertRejected(pending, ANA, assign("ASSIGN", "lucia"), ErrorCode.INVALID_TRANSITION,
					"Transition 'ASSIGN' is not allowed from state PENDING");
		}

	}

	private void assertRejected(FakeWorkflowItem item, User user, TransitionRequest request, ErrorCode errorCode,
			String message) {
		FakeWorkflowItem before = item.copy();
		ThrowingCallable apply = () -> engine.apply(item, user, request);

		assertThatThrownBy(apply).isInstanceOfSatisfying(ApiException.class,
				ex -> assertThat(ex.errorCode()).isEqualTo(errorCode))
			.hasMessage(message);
		assertThat(item).usingRecursiveComparison().isEqualTo(before);
	}

	private static TransitionRequest assign(String code, String assigneeId) {
		return new TransitionRequest(code, assigneeId, null, null);
	}

	private static TransitionRequest complete(Outcome outcome, String comment) {
		return new TransitionRequest("COMPLETE", null, outcome, comment);
	}

	private static TransitionRequest reopen(String reason) {
		return new TransitionRequest("REOPEN", null, null, reason);
	}

}
