package io.github.elmatty18.workflowinbox.workflow;

import java.util.Set;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;
import io.github.elmatty18.workflowinbox.users.User;

import org.springframework.stereotype.Component;

/**
 * {@code REOPEN}: requires a reason, which replaces the comment; the outcome is cleared.
 */
@Component
class ReopenTransitionHandler implements TransitionHandler {

	@Override
	public Set<String> codes() {
		return Set.of("REOPEN");
	}

	@Override
	public void validate(WorkflowItem item, TransitionRequest request, User user) {
		if (request.comment() == null) {
			throw new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, "comment (the reason) is required for REOPEN");
		}
	}

	@Override
	public void apply(WorkflowItem item, TransitionRequest request, User user) {
		item.reopen(request.comment());
	}

}
