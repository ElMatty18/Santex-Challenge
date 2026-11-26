package io.github.elmatty18.workflowinbox.workflow;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of the configured workflow. Available to every identified user.
 */
@Tag(name = "Workflow")
@RestController
@RequestMapping("/api/v1/workflow")
class WorkflowController {

	private final WorkflowDefinition definition;

	WorkflowController(WorkflowDefinition definition) {
		this.definition = definition;
	}

	@Operation(summary = "Configured workflow states")
	@GetMapping("/states")
	List<WorkflowState> states() {
		return definition.states();
	}

	@Operation(summary = "Configured transitions, one row per allowed role")
	@GetMapping("/transitions")
	List<Transition> transitions() {
		return definition.transitions();
	}

}
