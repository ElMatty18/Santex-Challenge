package io.github.elmatty18.workflowinbox.workflow;

import java.time.Instant;

/**
 * Something that moves through the workflow. Implemented by the {@code Action} entity, so the
 * engine and handlers never depend on the actions module.
 */
public interface WorkflowItem {

	int stateCode();

	String assigneeId();

	Outcome outcome();

	String comment();

	String completedBy();

	Instant completedAt();

	CompletionPolicy completionPolicy();

	void moveTo(int stateCode);

	void assignTo(String assigneeId);

	void complete(Outcome outcome, String comment, String completedBy, Instant completedAt);

	/**
	 * Stores the reason as the comment and clears outcome and completion data; keeps the assignee.
	 */
	void reopen(String reason);

}
