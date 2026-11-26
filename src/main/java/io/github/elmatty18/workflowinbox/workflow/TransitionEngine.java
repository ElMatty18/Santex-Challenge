package io.github.elmatty18.workflowinbox.workflow;

import java.util.List;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;
import io.github.elmatty18.workflowinbox.users.User;

import org.springframework.stereotype.Service;

/**
 * Applies transitions using the configured table (whether) and the handlers (what else).
 * Callers must check visibility (404) before calling: the engine assumes the user can see the item.
 */
@Service
public class TransitionEngine {

	private final WorkflowDefinition definition;

	private final TransitionHandlerRegistry handlers;

	TransitionEngine(WorkflowDefinition definition, TransitionHandlerRegistry handlers) {
		this.definition = definition;
		this.handlers = handlers;
	}

	public WorkflowState initialState() {
		return definition.initialState();
	}

	public WorkflowState state(int code) {
		return definition.state(code);
	}

	/**
	 * Transitions the user's role may start from the item's current state, according to the table.
	 * Handler preconditions are not evaluated, and idempotent repeats are not listed.
	 */
	public List<AvailableTransition> availableTransitions(WorkflowItem item, User user) {
		return definition.transitionsFrom(item.stateCode())
			.stream()
			.filter(t -> t.allowedRole() == user.role())
			.map(t -> new AvailableTransition(t.code(), t.toState(), definition.state(t.toState()).name()))
			.toList();
	}

	/**
	 * Validates and applies a transition, mutating the item.
	 * @return the result; {@code applied} is {@code false} for an idempotent repeat (item untouched)
	 * @throws ApiException {@code INVALID_TRANSITION} if the code is not defined from the current
	 * state, {@code FORBIDDEN} if it is but not for the user's role, {@code BUSINESS_RULE_VIOLATION}
	 * if a handler rule fails, or the handler's conflict (e.g. {@code ACTION_ALREADY_COMPLETED}) for
	 * a non-matching repeat
	 */
	public TransitionResult apply(WorkflowItem item, User user, TransitionRequest request) {
		WorkflowState from = definition.state(item.stateCode());
		TransitionHandler handler = handlers.handlerFor(request.code());
		List<Transition> candidates = matching(definition.transitionsFrom(from.code()), request.code());

		if (candidates.isEmpty()) {
			return applyRepeat(item, user, request, from, handler);
		}

		Transition transition = candidates.stream()
			.filter(t -> t.allowedRole() == user.role())
			.findFirst()
			.orElseThrow(() -> forbidden(user, request, from));

		handler.validate(item, request, user);
		handler.apply(item, request, user);
		item.moveTo(transition.toState());
		return new TransitionResult(request.code(), from, definition.state(transition.toState()), true);
	}

	/**
	 * The code does not leave the current state. For an idempotent handler whose transition leads
	 * into the current state, this may be a retry: same check order as a normal transition (role,
	 * then handler rules), then either "nothing to do" or the handler's conflict.
	 */
	private TransitionResult applyRepeat(WorkflowItem item, User user, TransitionRequest request,
			WorkflowState current, TransitionHandler handler) {
		List<Transition> into = matching(definition.transitionsInto(current.code()), request.code());
		if (!(handler instanceof IdempotentTransitionHandler idempotent) || into.isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_TRANSITION,
					"Transition '%s' is not allowed from state %s".formatted(request.code(), current.name()));
		}
		if (into.stream().noneMatch(t -> t.allowedRole() == user.role())) {
			throw forbidden(user, request, current);
		}
		idempotent.validate(item, request, user);
		if (!idempotent.isRepeatOf(item, request)) {
			throw idempotent.repeatConflict(item);
		}
		return new TransitionResult(request.code(), current, current, false);
	}

	private static List<Transition> matching(List<Transition> transitions, String code) {
		return transitions.stream().filter(t -> t.code().equals(code)).toList();
	}

	private static ApiException forbidden(User user, TransitionRequest request, WorkflowState from) {
		return new ApiException(ErrorCode.FORBIDDEN, "Role %s may not apply transition '%s' from state %s"
			.formatted(user.role(), request.code(), from.name()));
	}

}
