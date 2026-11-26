package io.github.elmatty18.workflowinbox.workflow;

import java.util.Set;

/**
 * Fallback for transition codes without a dedicated handler: no validation, no side effects. The
 * engine still changes the state, so a new code needs only migration rows. Not a Spring bean on
 * purpose: it is the registry's default, not a handler for a specific code.
 */
final class GenericTransitionHandler implements TransitionHandler {

	@Override
	public Set<String> codes() {
		return Set.of();
	}

}
