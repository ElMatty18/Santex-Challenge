package io.github.elmatty18.workflowinbox.workflow;

import io.github.elmatty18.workflowinbox.users.Role;

/**
 * One row of {@code state_transition}: {@code allowedRole} may move an item from
 * {@code fromState} to {@code toState} with {@code code}.
 */
public record Transition(String code, int fromState, int toState, Role allowedRole) {

	@Override
	public String toString() {
		return "%s (%d -> %d, %s)".formatted(code, fromState, toState, allowedRole);
	}

}
