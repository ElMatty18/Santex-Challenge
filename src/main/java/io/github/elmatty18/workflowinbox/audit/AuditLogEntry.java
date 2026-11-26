package io.github.elmatty18.workflowinbox.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * One changed field of one action. Insert-only: audit rows are never updated.
 */
@Entity
@Immutable
@Table(name = "audit_log")
class AuditLogEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "action_id", nullable = false)
	private long actionId;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;

	@Column(name = "user_id", nullable = false)
	private String userId;

	@Column(nullable = false)
	private String field;

	@Column(name = "old_value")
	private String oldValue;

	@Column(name = "new_value")
	private String newValue;

	@Column(name = "request_id")
	private String requestId;

	protected AuditLogEntry() {
		// for JPA
	}

	AuditLogEntry(long actionId, Instant changedAt, String userId, String field, String oldValue, String newValue,
			String requestId) {
		this.actionId = actionId;
		this.changedAt = changedAt;
		this.userId = userId;
		this.field = field;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.requestId = requestId;
	}

	AuditEntryResponse toResponse() {
		return new AuditEntryResponse(id, actionId, changedAt, userId, field, oldValue, newValue, requestId);
	}

}
