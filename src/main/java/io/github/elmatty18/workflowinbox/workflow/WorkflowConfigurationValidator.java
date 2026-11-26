package io.github.elmatty18.workflowinbox.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.elmatty18.workflowinbox.users.Role;

/**
 * Checks that a workflow configuration is usable (SPEC section 6, "Startup validation").
 * Collects every problem instead of stopping at the first one.
 */
final class WorkflowConfigurationValidator {

	private WorkflowConfigurationValidator() {
	}

	static List<String> validate(Collection<WorkflowState> states, Collection<Transition> transitions) {
		List<String> errors = new ArrayList<>();
		Map<Integer, WorkflowState> statesByCode = states.stream()
			.collect(Collectors.toMap(WorkflowState::code, Function.identity()));

		List<String> initialStates = states.stream().filter(WorkflowState::initial).map(WorkflowState::name).toList();
		if (initialStates.size() != 1) {
			errors.add("Expected exactly one initial state, found %d: %s".formatted(initialStates.size(), initialStates));
		}

		for (Transition transition : transitions) {
			WorkflowState from = statesByCode.get(transition.fromState());
			if (from == null) {
				errors.add("Transition %s references unknown state %d".formatted(transition, transition.fromState()));
			}
			if (!statesByCode.containsKey(transition.toState())) {
				errors.add("Transition %s references unknown state %d".formatted(transition, transition.toState()));
			}
			if (from != null && from.finalState() && transition.allowedRole() != Role.ADMIN) {
				errors.add("Transition %s from final state %s is allowed for %s; only ADMIN may leave a final state"
					.formatted(transition, from.name(), transition.allowedRole()));
			}
		}

		Map<String, Set<Integer>> targetsByCodeAndSource = transitions.stream()
			.collect(Collectors.groupingBy(t -> t.code() + " from " + t.fromState(),
					Collectors.mapping(Transition::toState, Collectors.toCollection(TreeSet::new))));
		targetsByCodeAndSource.forEach((codeAndSource, targets) -> {
			if (targets.size() > 1) {
				errors.add("Transition %s has multiple targets: %s".formatted(codeAndSource, targets));
			}
		});

		return errors;
	}

}
