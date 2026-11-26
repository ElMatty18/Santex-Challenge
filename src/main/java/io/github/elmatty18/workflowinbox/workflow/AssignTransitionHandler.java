package io.github.elmatty18.workflowinbox.workflow;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;
import io.github.elmatty18.workflowinbox.users.Role;
import io.github.elmatty18.workflowinbox.users.User;
import io.github.elmatty18.workflowinbox.users.UserDirectory;

import org.springframework.stereotype.Component;

/**
 * {@code ASSIGN} and {@code REASSIGN}: the assignee must be an existing reviewer, different from
 * the current one.
 */
@Component
class AssignTransitionHandler implements TransitionHandler {

	private final UserDirectory userDirectory;

	AssignTransitionHandler(UserDirectory userDirectory) {
		this.userDirectory = userDirectory;
	}

	@Override
	public Set<String> codes() {
		return Set.of("ASSIGN", "REASSIGN");
	}

	@Override
	public void validate(WorkflowItem item, TransitionRequest request, User user) {
		String assigneeId = request.assigneeId();
		if (assigneeId == null) {
			throw violation("assigneeId is required for " + request.code());
		}
		boolean isReviewer = userDirectory.findById(assigneeId).filter(u -> u.hasRole(Role.REVIEWER)).isPresent();
		if (!isReviewer) {
			throw violation("Assignee '%s' must be an existing user with role REVIEWER".formatted(assigneeId));
		}
		if (Objects.equals(assigneeId, item.assigneeId())) {
			throw violation("Action is already assigned to '%s'".formatted(assigneeId));
		}
	}

	@Override
	public void apply(WorkflowItem item, TransitionRequest request, User user, Instant occurredAt) {
		item.assignTo(request.assigneeId());
	}

	private static ApiException violation(String message) {
		return new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, message);
	}

}
