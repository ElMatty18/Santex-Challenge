package io.github.elmatty18.workflowinbox.workflow;

import java.util.List;

/**
 * Thrown at startup when {@code workflow_state} / {@code state_transition} are inconsistent.
 * Reported by {@link InvalidWorkflowConfigurationFailureAnalyzer}.
 */
public class InvalidWorkflowConfigurationException extends RuntimeException {

	private final List<String> errors;

	InvalidWorkflowConfigurationException(List<String> errors) {
		super("Invalid workflow configuration:%n - %s".formatted(String.join("%n - ".formatted(), errors)));
		this.errors = List.copyOf(errors);
	}

	public List<String> errors() {
		return errors;
	}

}
