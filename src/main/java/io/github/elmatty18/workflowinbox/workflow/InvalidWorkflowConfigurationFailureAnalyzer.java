package io.github.elmatty18.workflowinbox.workflow;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns an invalid workflow configuration into Spring Boot's "APPLICATION FAILED TO START"
 * report instead of a stack trace. Registered in {@code META-INF/spring.factories}.
 */
class InvalidWorkflowConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<InvalidWorkflowConfigurationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, InvalidWorkflowConfigurationException cause) {
		String description = "The workflow configuration in workflow_state / state_transition is invalid:%n - %s"
			.formatted(String.join("%n - ".formatted(), cause.errors()));
		String action = "Fix the workflow rows with a new Flyway migration (src/main/resources/db/migration) and restart.";
		return new FailureAnalysis(description, action, cause);
	}

}
