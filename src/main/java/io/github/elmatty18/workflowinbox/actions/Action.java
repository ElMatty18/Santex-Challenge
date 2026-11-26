package io.github.elmatty18.workflowinbox.actions;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import io.github.elmatty18.workflowinbox.workflow.CompletionPolicy;
import io.github.elmatty18.workflowinbox.workflow.Outcome;
import io.github.elmatty18.workflowinbox.workflow.WorkflowItem;

/**
 * An item that needs a human decision. State, assignee, outcome and comment only change through
 * the {@link WorkflowItem} methods, which the transition engine and handlers call.
 */
@Entity
@Table(name = "action")
class Action implements WorkflowItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ActionType type;

	@Column(nullable = false)
	private String title;

	private String description;

	@Column(name = "state_code", nullable = false)
	private int stateCode;

	@Column(name = "requester_id", nullable = false)
	private String requesterId;

	@Column(name = "assignee_id")
	private String assigneeId;

	@Enumerated(EnumType.STRING)
	private Outcome outcome;

	private String comment;

	@Column(name = "completed_by")
	private String completedBy;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private Long version;

	protected Action() {
		// for JPA
	}

	static Action create(ActionType type, String title, String description, String requesterId, int initialStateCode,
			Instant now) {
		Action action = new Action();
		action.type = type;
		action.title = title;
		action.description = description;
		action.requesterId = requesterId;
		action.stateCode = initialStateCode;
		action.createdAt = now;
		action.updatedAt = now;
		return action;
	}

	void touch(Instant now) {
		this.updatedAt = now;
	}

	// --- WorkflowItem

	@Override
	public int stateCode() {
		return stateCode;
	}

	@Override
	public String assigneeId() {
		return assigneeId;
	}

	@Override
	public Outcome outcome() {
		return outcome;
	}

	@Override
	public String comment() {
		return comment;
	}

	@Override
	public String completedBy() {
		return completedBy;
	}

	@Override
	public Instant completedAt() {
		return completedAt;
	}

	@Override
	public CompletionPolicy completionPolicy() {
		return type;
	}

	@Override
	public void moveTo(int stateCode) {
		this.stateCode = stateCode;
	}

	@Override
	public void assignTo(String assigneeId) {
		this.assigneeId = assigneeId;
	}

	@Override
	public void complete(Outcome outcome, String comment, String completedBy, Instant completedAt) {
		this.outcome = outcome;
		this.comment = comment;
		this.completedBy = completedBy;
		this.completedAt = completedAt;
	}

	@Override
	public void reopen(String reason) {
		this.comment = reason;
		this.outcome = null;
		this.completedBy = null;
		this.completedAt = null;
	}

	// --- read access for mapping

	Long id() {
		return id;
	}

	ActionType type() {
		return type;
	}

	String title() {
		return title;
	}

	String description() {
		return description;
	}

	String requesterId() {
		return requesterId;
	}

	Instant createdAt() {
		return createdAt;
	}

	Instant updatedAt() {
		return updatedAt;
	}

	Long version() {
		return version;
	}

}
