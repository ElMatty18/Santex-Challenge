package io.github.elmatty18.workflowinbox.workflow;

import static io.github.elmatty18.workflowinbox.workflow.TestWorkflows.COMPLETED;
import static io.github.elmatty18.workflowinbox.workflow.TestWorkflows.CREATED;
import static io.github.elmatty18.workflowinbox.workflow.TestWorkflows.PENDING;
import static io.github.elmatty18.workflowinbox.workflow.TestWorkflows.seededStates;
import static io.github.elmatty18.workflowinbox.workflow.TestWorkflows.seededTransitions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import io.github.elmatty18.workflowinbox.users.Role;

import org.junit.jupiter.api.Test;

class WorkflowConfigurationValidatorTests {

	@Test
	void seededConfigurationIsValid() {
		assertThat(WorkflowConfigurationValidator.validate(seededStates(), seededTransitions())).isEmpty();
	}

	@Test
	void rejectsNoInitialState() {
		List<WorkflowState> states = List.of(new WorkflowState(CREATED, "CREATED", false, false),
				new WorkflowState(PENDING, "PENDING", false, false),
				new WorkflowState(COMPLETED, "COMPLETED", false, true));

		assertThat(WorkflowConfigurationValidator.validate(states, seededTransitions()))
			.containsExactly("Expected exactly one initial state, found 0: []");
	}

	@Test
	void rejectsTwoInitialStates() {
		List<WorkflowState> states = with(seededStates(), new WorkflowState(5, "DRAFT", true, false));

		assertThat(WorkflowConfigurationValidator.validate(states, seededTransitions()))
			.containsExactly("Expected exactly one initial state, found 2: [CREATED, DRAFT]");
	}

	@Test
	void rejectsTransitionFromUnknownState() {
		List<Transition> transitions = with(seededTransitions(), new Transition("ESCALATE", 42, PENDING, Role.ADMIN));

		assertThat(WorkflowConfigurationValidator.validate(seededStates(), transitions))
			.containsExactly("Transition ESCALATE (42 -> 10, ADMIN) references unknown state 42");
	}

	@Test
	void rejectsTransitionToUnknownState() {
		List<Transition> transitions = with(seededTransitions(), new Transition("ARCHIVE", PENDING, 99, Role.ADMIN));

		assertThat(WorkflowConfigurationValidator.validate(seededStates(), transitions))
			.containsExactly("Transition ARCHIVE (10 -> 99, ADMIN) references unknown state 99");
	}

	@Test
	void rejectsNonAdminTransitionFromFinalState() {
		List<Transition> transitions = with(seededTransitions(),
				new Transition("REOPEN", COMPLETED, PENDING, Role.REVIEWER));

		assertThat(WorkflowConfigurationValidator.validate(seededStates(), transitions)).containsExactly(
				"Transition REOPEN (100 -> 10, REVIEWER) from final state COMPLETED is allowed for REVIEWER; "
						+ "only ADMIN may leave a final state");
	}

	@Test
	void allowsAdminTransitionFromFinalState() {
		// the seeded REOPEN row is exactly this case
		assertThat(seededTransitions()).contains(new Transition("REOPEN", COMPLETED, PENDING, Role.ADMIN));
		assertThat(WorkflowConfigurationValidator.validate(seededStates(), seededTransitions())).isEmpty();
	}

	@Test
	void rejectsSameCodeAndSourceWithSeveralTargets() {
		List<WorkflowState> states = with(seededStates(), new WorkflowState(50, "IN_REVIEW", false, false));
		List<Transition> transitions = with(seededTransitions(), new Transition("COMPLETE", PENDING, 50, Role.ADMIN));

		assertThat(WorkflowConfigurationValidator.validate(states, transitions))
			.containsExactly("Transition COMPLETE from 10 has multiple targets: [50, 100]");
	}

	@Test
	void reportsAllErrorsTogether() {
		List<WorkflowState> states = List.of(new WorkflowState(PENDING, "PENDING", false, false),
				new WorkflowState(COMPLETED, "COMPLETED", false, true));
		List<Transition> transitions = List.of(new Transition("ASSIGN", CREATED, PENDING, Role.REQUESTER),
				new Transition("REOPEN", COMPLETED, PENDING, Role.REVIEWER));

		assertThat(WorkflowConfigurationValidator.validate(states, transitions)).hasSize(3);
	}

	@Test
	void definitionRefusesInvalidConfiguration() {
		List<Transition> transitions = with(seededTransitions(),
				new Transition("REOPEN", COMPLETED, PENDING, Role.REVIEWER));

		assertThatThrownBy(() -> WorkflowDefinition.of(seededStates(), transitions))
			.isInstanceOfSatisfying(InvalidWorkflowConfigurationException.class,
					ex -> assertThat(ex.errors()).hasSize(1))
			.hasMessageContaining("only ADMIN may leave a final state");
	}

	private static <T> List<T> with(List<T> list, T extra) {
		List<T> copy = new ArrayList<>(list);
		copy.add(extra);
		return copy;
	}

}
