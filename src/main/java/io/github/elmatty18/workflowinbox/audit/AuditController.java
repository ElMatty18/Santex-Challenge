package io.github.elmatty18.workflowinbox.audit;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.github.elmatty18.workflowinbox.shared.web.PageResponse;
import io.github.elmatty18.workflowinbox.users.CurrentUser;
import io.github.elmatty18.workflowinbox.users.User;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
class AuditController {

	private final AuditService service;

	AuditController(AuditService service) {
		this.service = service;
	}

	/**
	 * {@code from} and {@code to} are ISO-8601 instants, e.g. {@code 2026-11-19T00:00:00Z}.
	 */
	@GetMapping
	PageResponse<AuditEntryResponse> search(@RequestParam(required = false) Long actionId,
			@RequestParam(required = false) String userId, @RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, @CurrentUser User user) {
		return service.search(actionId, userId, from, to, page, size, user);
	}

}
