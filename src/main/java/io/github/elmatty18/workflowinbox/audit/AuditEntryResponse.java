package io.github.elmatty18.workflowinbox.audit;

import java.time.Instant;

record AuditEntryResponse(Long id, long actionId, Instant changedAt, String userId, String field, String oldValue,
		String newValue, String requestId) {

}
