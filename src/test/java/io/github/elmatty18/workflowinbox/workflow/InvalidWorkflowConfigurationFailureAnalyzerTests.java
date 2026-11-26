package io.github.elmatty18.workflowinbox.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.io.support.SpringFactoriesLoader;

class InvalidWorkflowConfigurationFailureAnalyzerTests {

	@Test
	void describesEveryErrorAndHowToFixIt() {
		var cause = new InvalidWorkflowConfigurationException(
				List.of("Expected exactly one initial state, found 0: []", "Transition X (1 -> 2, ADMIN) references unknown state 2"));
		var startupFailure = new BeanCreationException("workflowDefinition", "Failed to instantiate", cause);

		FailureAnalysis analysis = new InvalidWorkflowConfigurationFailureAnalyzer().analyze(startupFailure);

		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).contains("Expected exactly one initial state, found 0: []")
			.contains("references unknown state 2");
		assertThat(analysis.getAction()).contains("Flyway migration");
	}

	@Test
	void isRegisteredWithSpringBoot() {
		// some of Boot's own analyzers need a BeanFactory to be instantiated: skip them
		SpringFactoriesLoader.FailureHandler skipUninstantiable = (factoryType, implementationName, failure) -> {
		};

		assertThat(SpringFactoriesLoader.forDefaultResourceLocation().load(FailureAnalyzer.class, skipUninstantiable))
			.hasAtLeastOneElementOfType(InvalidWorkflowConfigurationFailureAnalyzer.class);
	}

}
