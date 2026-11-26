package io.github.elmatty18.workflowinbox.audit;

import io.github.elmatty18.workflowinbox.shared.events.ActionChangedEvent;
import io.github.elmatty18.workflowinbox.shared.web.RequestIdFilter;

import org.slf4j.MDC;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes one audit row per changed field, synchronously and in the publisher's transaction: a
 * change and its audit commit or roll back together. {@code MANDATORY} makes publishing outside a
 * transaction fail loudly instead of auditing without one.
 */
@Component
class AuditEventListener {

	private final AuditLogRepository repository;

	AuditEventListener(AuditLogRepository repository) {
		this.repository = repository;
	}

	@EventListener
	@Transactional(propagation = Propagation.MANDATORY)
	void on(ActionChangedEvent event) {
		// same thread as the request, so the MDC still holds its id (null outside a web request)
		String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
		repository.saveAll(event.changes()
			.stream()
			.map(change -> new AuditLogEntry(event.actionId(), event.occurredAt(), event.userId(), change.field(),
					change.oldValue(), change.newValue(), requestId))
			.toList());
	}

}
