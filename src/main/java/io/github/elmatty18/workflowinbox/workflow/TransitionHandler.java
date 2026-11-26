package io.github.elmatty18.workflowinbox.workflow;

import java.util.Set;

import io.github.elmatty18.workflowinbox.users.User;

/**
 * Strategy for what else happens on a transition. The {@code state_transition} table decides
 * whether a transition is allowed; a handler validates the request and applies side effects.
 * The engine changes the state itself, so codes without a handler need no code at all.
 */
public interface TransitionHandler {

	/**
	 * @return the transition codes this handler serves
	 */
	Set<String> codes();

	/**
	 * Called before any change; must not modify the item.
	 * @throws io.github.elmatty18.workflowinbox.shared.error.ApiException with
	 * {@code BUSINESS_RULE_VIOLATION} if the request is not acceptable
	 */
	default void validate(WorkflowItem item, TransitionRequest request, User user) {
	}

	/**
	 * Applies side effects. Called only after {@link #validate} passed.
	 */
	default void apply(WorkflowItem item, TransitionRequest request, User user) {
	}

}
