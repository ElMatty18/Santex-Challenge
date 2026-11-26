package io.github.elmatty18.workflowinbox.workflow;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

/**
 * Loads and validates the workflow once at startup. An invalid configuration fails the
 * application context (fail fast), see {@link InvalidWorkflowConfigurationFailureAnalyzer}.
 */
@Configuration(proxyBeanMethods = false)
class WorkflowDefinitionLoader {

	@Bean
	WorkflowDefinition workflowDefinition(WorkflowStateRepository states, StateTransitionRepository transitions) {
		return WorkflowDefinition.of(
				states.findAll().stream().map(WorkflowStateEntity::toState).toList(),
				transitions.findAll(Sort.by("id")).stream().map(StateTransitionEntity::toTransition).toList());
	}

}
