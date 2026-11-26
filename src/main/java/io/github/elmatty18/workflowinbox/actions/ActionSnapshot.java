package io.github.elmatty18.workflowinbox.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.elmatty18.workflowinbox.shared.events.FieldChange;

/**
 * The audited fields of an action at one point in time, as text. Comparing two snapshots gives
 * the field changes to publish, in a fixed order (assignee, state, outcome, comment).
 */
record ActionSnapshot(String state, String assignee, String outcome, String comment) {

	/** Before creation: every field empty. */
	static final ActionSnapshot NONE = new ActionSnapshot(null, null, null, null);

	static ActionSnapshot of(Action action, String stateName) {
		return new ActionSnapshot(stateName, action.assigneeId(),
				action.outcome() == null ? null : action.outcome().name(), action.comment());
	}

	List<FieldChange> changesTo(ActionSnapshot after) {
		List<FieldChange> changes = new ArrayList<>();
		addIfChanged(changes, "assignee", assignee, after.assignee);
		addIfChanged(changes, "state", state, after.state);
		addIfChanged(changes, "outcome", outcome, after.outcome);
		addIfChanged(changes, "comment", comment, after.comment);
		return changes;
	}

	private static void addIfChanged(List<FieldChange> changes, String field, String before, String after) {
		if (!Objects.equals(before, after)) {
			changes.add(new FieldChange(field, before, after));
		}
	}

}
