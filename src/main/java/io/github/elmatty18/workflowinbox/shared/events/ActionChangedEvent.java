package io.github.elmatty18.workflowinbox.shared.events;

import java.time.Instant;
import java.util.List;

/**
 * Published by the actions module, inside the transaction that changed the action. Lives in
 * {@code shared} so consumers (audit) never depend on the actions module.
 */
public record ActionChangedEvent(long actionId, String userId, List<FieldChange> changes, Instant occurredAt) {

	public ActionChangedEvent {
		changes = List.copyOf(changes);
	}

}
