package io.github.elmatty18.workflowinbox.workflow;

import java.time.Clock;
import java.util.Map;
import java.util.Set;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;
import io.github.elmatty18.workflowinbox.users.User;

import org.springframework.stereotype.Component;

/**
 * {@code COMPLETE}: records the human decision. The outcome rules per item come from its
 * {@link CompletionPolicy}. Idempotent: repeating it with the same outcome changes nothing.
 */
@Component
class CompleteTransitionHandler implements IdempotentTransitionHandler {

	private final Clock clock;

	CompleteTransitionHandler(Clock clock) {
		this.clock = clock;
	}

	@Override
	public Set<String> codes() {
		return Set.of("COMPLETE");
	}

	@Override
	public void validate(WorkflowItem item, TransitionRequest request, User user) {
		if (request.outcome() == null) {
			throw new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, "outcome is required for COMPLETE");
		}
		item.completionPolicy().validate(request.outcome(), request.comment());
	}

	@Override
	public void apply(WorkflowItem item, TransitionRequest request, User user) {
		item.complete(request.outcome(), request.comment(), user.id(), clock.instant());
	}

	@Override
	public boolean isRepeatOf(WorkflowItem item, TransitionRequest request) {
		return request.outcome() == item.outcome();
	}

	@Override
	public ApiException repeatConflict(WorkflowItem item) {
		return new ApiException(ErrorCode.ACTION_ALREADY_COMPLETED,
				"Action was already completed with outcome %s by '%s' at %s"
					.formatted(item.outcome(), item.completedBy(), item.completedAt()),
				Map.of("outcome", item.outcome(), "completedBy", item.completedBy(), "completedAt",
						item.completedAt()));
	}

}
