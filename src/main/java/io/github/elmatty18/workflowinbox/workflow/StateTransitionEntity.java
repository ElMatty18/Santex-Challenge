package io.github.elmatty18.workflowinbox.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.github.elmatty18.workflowinbox.users.Role;

import org.hibernate.annotations.Immutable;

/**
 * From/to are plain codes rather than associations so that the startup validator, not Hibernate,
 * decides what an unknown state means.
 */
@Entity
@Immutable
@Table(name = "state_transition")
class StateTransitionEntity {

	@Id
	private Long id;

	@Column(nullable = false)
	private String code;

	@Column(name = "from_state", nullable = false)
	private int fromState;

	@Column(name = "to_state", nullable = false)
	private int toState;

	@Enumerated(EnumType.STRING)
	@Column(name = "allowed_role", nullable = false)
	private Role allowedRole;

	protected StateTransitionEntity() {
		// for JPA
	}

	Transition toTransition() {
		return new Transition(code, fromState, toState, allowedRole);
	}

}
