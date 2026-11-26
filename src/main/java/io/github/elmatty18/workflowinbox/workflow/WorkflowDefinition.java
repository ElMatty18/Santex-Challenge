package io.github.elmatty18.workflowinbox.workflow;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Immutable, validated snapshot of the configured states and transitions. Loaded once at startup:
 * the workflow only changes through migrations, which require a restart.
 */
public final class WorkflowDefinition {

	private final Map<Integer, WorkflowState> statesByCode;

	private final List<WorkflowState> states;

	private final List<Transition> transitions;

	private final WorkflowState initialState;

	private WorkflowDefinition(Collection<WorkflowState> states, Collection<Transition> transitions) {
		this.states = states.stream().sorted(Comparator.comparingInt(WorkflowState::code)).toList();
		this.statesByCode = this.states.stream().collect(Collectors.toMap(WorkflowState::code, Function.identity()));
		this.transitions = List.copyOf(transitions);
		this.initialState = this.states.stream().filter(WorkflowState::initial).findFirst().orElseThrow();
	}

	/**
	 * @throws InvalidWorkflowConfigurationException if the configuration fails validation
	 */
	public static WorkflowDefinition of(Collection<WorkflowState> states, Collection<Transition> transitions) {
		List<String> errors = WorkflowConfigurationValidator.validate(states, transitions);
		if (!errors.isEmpty()) {
			throw new InvalidWorkflowConfigurationException(errors);
		}
		return new WorkflowDefinition(states, transitions);
	}

	public List<WorkflowState> states() {
		return states;
	}

	public List<Transition> transitions() {
		return transitions;
	}

	public WorkflowState initialState() {
		return initialState;
	}

	/**
	 * @throws IllegalArgumentException if no state has this code (cannot happen for persisted
	 * items, which reference {@code workflow_state} through a foreign key)
	 */
	public WorkflowState state(int code) {
		WorkflowState state = statesByCode.get(code);
		if (state == null) {
			throw new IllegalArgumentException("Unknown workflow state " + code);
		}
		return state;
	}

	public List<Transition> transitionsFrom(int stateCode) {
		return transitions.stream().filter(t -> t.fromState() == stateCode).toList();
	}

	public List<Transition> transitionsInto(int stateCode) {
		return transitions.stream().filter(t -> t.toState() == stateCode).toList();
	}

}
