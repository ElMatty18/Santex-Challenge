package io.github.elmatty18.workflowinbox.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "workflow_state")
class WorkflowStateEntity {

	@Id
	private Integer code;

	@Column(nullable = false)
	private String name;

	@Column(name = "is_initial", nullable = false)
	private boolean initial;

	@Column(name = "is_final", nullable = false)
	private boolean finalState;

	protected WorkflowStateEntity() {
		// for JPA
	}

	WorkflowState toState() {
		return new WorkflowState(code, name, initial, finalState);
	}

}
