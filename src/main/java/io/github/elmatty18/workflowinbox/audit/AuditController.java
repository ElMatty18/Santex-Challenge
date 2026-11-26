package io.github.elmatty18.workflowinbox.audit;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.github.elmatty18.workflowinbox.shared.web.OpenApiConfiguration;
import io.github.elmatty18.workflowinbox.shared.web.PageResponse;
import io.github.elmatty18.workflowinbox.users.CurrentUser;
import io.github.elmatty18.workflowinbox.users.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit")
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
	@Operation(summary = "Search the audit log",
			description = "ADMIN only. One row per changed field, newest first. from/to are inclusive ISO-8601 instants.")
	@ApiResponse(responseCode = "403", ref = OpenApiConfiguration.FORBIDDEN)
	@GetMapping
	PageResponse<AuditEntryResponse> search(@RequestParam(required = false) Long actionId,
			@RequestParam(required = false) String userId, @RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, @CurrentUser User user) {
		return service.search(actionId, userId, from, to, page, size, user);
	}

}
