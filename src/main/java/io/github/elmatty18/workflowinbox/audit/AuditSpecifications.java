package io.github.elmatty18.workflowinbox.audit;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

final class AuditSpecifications {

	private AuditSpecifications() {
	}

	static Specification<AuditLogEntry> forAction(long actionId) {
		return (root, query, cb) -> cb.equal(root.get("actionId"), actionId);
	}

	static Specification<AuditLogEntry> byUser(String userId) {
		return (root, query, cb) -> cb.equal(root.get("userId"), userId);
	}

	/** Inclusive. */
	static Specification<AuditLogEntry> changedFrom(Instant from) {
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("changedAt"), from);
	}

	/** Inclusive. */
	static Specification<AuditLogEntry> changedTo(Instant to) {
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("changedAt"), to);
	}

}
