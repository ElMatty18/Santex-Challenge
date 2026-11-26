package io.github.elmatty18.workflowinbox.workflow;

import static io.github.elmatty18.workflowinbox.users.Role.ADMIN;
import static io.github.elmatty18.workflowinbox.users.Role.REQUESTER;
import static io.github.elmatty18.workflowinbox.users.Role.REVIEWER;

import java.util.ArrayList;
import java.util.List;

import io.github.elmatty18.workflowinbox.users.Role;
import io.github.elmatty18.workflowinbox.users.User;

/**
 * Workflow configuration and users mirroring the Flyway seed (V2, V3), for unit tests.
 */
final class TestWorkflows {

	static final int CREATED = 0;

	static final int PENDING = 10;

	static final int COMPLETED = 100;

	static final User ADMIN_USER = new User("admin", "Admin", ADMIN);

	static final User ANA = new User("ana", "Ana Requester", REQUESTER);

	static final User LUCIA = new User("lucia", "Lucia Reviewer", REVIEWER);

	static final User MARTIN = new User("martin", "Martin Reviewer", REVIEWER);

	static final List<User> USERS = List.of(ADMIN_USER, ANA, LUCIA, MARTIN);

	private TestWorkflows() {
	}

	static List<WorkflowState> seededStates() {
		return List.of(new WorkflowState(CREATED, "CREATED", true, false),
				new WorkflowState(PENDING, "PENDING", false, false),
				new WorkflowState(COMPLETED, "COMPLETED", false, true));
	}

	static List<Transition> seededTransitions() {
		return List.of(new Transition("ASSIGN", CREATED, PENDING, REQUESTER),
				new Transition("ASSIGN", CREATED, PENDING, ADMIN),
				new Transition("REASSIGN", PENDING, PENDING, ADMIN),
				new Transition("COMPLETE", PENDING, COMPLETED, REVIEWER),
				new Transition("COMPLETE", PENDING, COMPLETED, ADMIN),
				new Transition("REOPEN", COMPLETED, PENDING, ADMIN));
	}

	static WorkflowDefinition seeded() {
		return WorkflowDefinition.of(seededStates(), seededTransitions());
	}

	/**
	 * The seeded workflow plus an intermediate state reached with a code that has no handler.
	 */
	static WorkflowDefinition seededWithInReview() {
		List<WorkflowState> states = new ArrayList<>(seededStates());
		states.add(new WorkflowState(50, "IN_REVIEW", false, false));
		List<Transition> transitions = new ArrayList<>(seededTransitions());
		transitions.add(new Transition("START_REVIEW", PENDING, 50, Role.REVIEWER));
		return WorkflowDefinition.of(states, transitions);
	}

}
