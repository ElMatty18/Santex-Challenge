package io.github.elmatty18.workflowinbox.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;
import io.github.elmatty18.workflowinbox.workflow.Outcome;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The outcome rules of SPEC section 7. An empty comment column means no comment.
 */
class ActionTypeTests {

	@ParameterizedTest(name = "{0} {1} comment={2} -> valid={3}")
	@CsvSource({
			"EXPENSE_APPROVAL,  APPROVED, ,              true",
			"EXPENSE_APPROVAL,  REJECTED, Over budget,   true",
			"EXPENSE_APPROVAL,  REJECTED, ,              false",
			"EXPENSE_APPROVAL,  DONE,     ,              false",
			"DEPLOYMENT_REVIEW, APPROVED, Looks good,    true",
			"DEPLOYMENT_REVIEW, REJECTED, Missing tests, true",
			"DEPLOYMENT_REVIEW, APPROVED, ,              false",
			"DEPLOYMENT_REVIEW, REJECTED, ,              false",
			"DEPLOYMENT_REVIEW, DONE,     Whatever,      false",
			"DOCUMENT_UPLOAD,   DONE,     ,              true",
			"DOCUMENT_UPLOAD,   DONE,     Uploaded,      true",
			"DOCUMENT_UPLOAD,   APPROVED, ,              false",
			"ONBOARDING,        DONE,     ,              true",
			"ONBOARDING,        REJECTED, Not needed,    false" })
	void appliesOutcomeRules(ActionType type, Outcome outcome, String comment, boolean valid) {
		if (valid) {
			assertThatCode(() -> type.validate(outcome, comment)).doesNotThrowAnyException();
		}
		else {
			assertThatThrownBy(() -> type.validate(outcome, comment)).isInstanceOfSatisfying(ApiException.class,
					ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION));
		}
	}

}
