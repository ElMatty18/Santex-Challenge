package io.github.elmatty18.workflowinbox.workflow;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of the configured workflow. Available to every identified user.
 */
@RestController
@RequestMapping("/api/v1/workflow")
class WorkflowController {

	private final WorkflowDefinition definition;

	WorkflowController(WorkflowDefinition definition) {
		this.definition = definition;
	}

	@GetMapping("/states")
	List<WorkflowState> states() {
		return definition.states();
	}

	@GetMapping("/transitions")
	List<Transition> transitions() {
		return definition.transitions();
	}

}
