package io.github.elmatty18.workflowinbox.actions;

import static io.github.elmatty18.workflowinbox.workflow.Outcome.APPROVED;
import static io.github.elmatty18.workflowinbox.workflow.Outcome.DONE;
import static io.github.elmatty18.workflowinbox.workflow.Outcome.REJECTED;

import java.util.Set;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;
import io.github.elmatty18.workflowinbox.workflow.CompletionPolicy;
import io.github.elmatty18.workflowinbox.workflow.Outcome;

/**
 * Kind of action, and the outcome rules that apply when it is completed (SPEC section 7).
 */
public enum ActionType implements CompletionPolicy {

	EXPENSE_APPROVAL(Set.of(APPROVED, REJECTED), CommentRule.REQUIRED_WHEN_REJECTED),
	DEPLOYMENT_REVIEW(Set.of(APPROVED, REJECTED), CommentRule.ALWAYS_REQUIRED),
	DOCUMENT_UPLOAD(Set.of(DONE), CommentRule.OPTIONAL),
	ONBOARDING(Set.of(DONE), CommentRule.OPTIONAL);

	private final Set<Outcome> validOutcomes;

	private final CommentRule commentRule;

	ActionType(Set<Outcome> validOutcomes, CommentRule commentRule) {
		this.validOutcomes = validOutcomes;
		this.commentRule = commentRule;
	}

	@Override
	public void validate(Outcome outcome, String comment) {
		if (!validOutcomes.contains(outcome)) {
			throw violation("Outcome %s is not valid for %s; expected one of %s".formatted(outcome, this,
					validOutcomes.stream().sorted().toList()));
		}
		if (comment == null && commentRule.requiresComment(outcome)) {
			throw violation("comment is required for %s with outcome %s".formatted(this, outcome));
		}
	}

	private static ApiException violation(String message) {
		return new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, message);
	}

	private enum CommentRule {

		OPTIONAL, REQUIRED_WHEN_REJECTED, ALWAYS_REQUIRED;

		boolean requiresComment(Outcome outcome) {
			return switch (this) {
				case OPTIONAL -> false;
				case REQUIRED_WHEN_REJECTED -> outcome == REJECTED;
				case ALWAYS_REQUIRED -> true;
			};
		}

	}

}
