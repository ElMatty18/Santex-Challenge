package io.github.elmatty18.workflowinbox.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;
import io.github.elmatty18.workflowinbox.shared.web.PageResponse;
import io.github.elmatty18.workflowinbox.users.Role;
import io.github.elmatty18.workflowinbox.users.User;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuditService {

	private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("changedAt"), Sort.Order.desc("id"));

	private final AuditLogRepository repository;

	AuditService(AuditLogRepository repository) {
		this.repository = repository;
	}

	/**
	 * Admin-only search; every filter is optional and {@code from}/{@code to} are inclusive.
	 */
	@Transactional(readOnly = true)
	PageResponse<AuditEntryResponse> search(Long actionId, String userId, Instant from, Instant to, int page, int size,
			User user) {
		if (!user.hasRole(Role.ADMIN)) {
			throw new ApiException(ErrorCode.FORBIDDEN, "Only ADMIN may search the audit log");
		}
		if (from != null && to != null && from.isAfter(to)) {
			throw ApiException.validation("from", "must not be after 'to'");
		}
		List<Specification<AuditLogEntry>> conditions = new ArrayList<>();
		if (actionId != null) {
			conditions.add(AuditSpecifications.forAction(actionId));
		}
		if (userId != null) {
			conditions.add(AuditSpecifications.byUser(userId));
		}
		if (from != null) {
			conditions.add(AuditSpecifications.changedFrom(from));
		}
		if (to != null) {
			conditions.add(AuditSpecifications.changedTo(to));
		}
		return PageResponse.from(
				repository.findAll(Specification.allOf(conditions), PageRequest.of(page, size, NEWEST_FIRST)),
				AuditLogEntry::toResponse);
	}

}
