package io.github.elmatty18.workflowinbox.actions;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.github.elmatty18.workflowinbox.actions.ActionDtos.ActionResponse;
import io.github.elmatty18.workflowinbox.actions.ActionDtos.CreateActionRequest;
import io.github.elmatty18.workflowinbox.actions.ActionDtos.TransitionCommand;
import io.github.elmatty18.workflowinbox.shared.web.OpenApiConfiguration;
import io.github.elmatty18.workflowinbox.shared.web.PageResponse;
import io.github.elmatty18.workflowinbox.users.CurrentUser;
import io.github.elmatty18.workflowinbox.users.User;
import io.github.elmatty18.workflowinbox.workflow.AvailableTransition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Actions")
@RestController
@RequestMapping("/api/v1/actions")
class ActionController {

	private final ActionService service;

	ActionController(ActionService service) {
		this.service = service;
	}

	@Operation(summary = "Create an action",
			description = "REQUESTER or ADMIN. Starts in the initial state; with assigneeId, ASSIGN is applied in the same transaction.")
	@ApiResponse(responseCode = "201", description = "Created; Location points to the new action")
	@ApiResponse(responseCode = "403", ref = OpenApiConfiguration.FORBIDDEN)
	@ApiResponse(responseCode = "422", ref = OpenApiConfiguration.UNPROCESSABLE)
	@PostMapping
	ResponseEntity<ActionResponse> create(@Valid @RequestBody CreateActionRequest request, @CurrentUser User user) {
		ActionResponse created = service.create(request, user);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@Operation(summary = "Inbox",
			description = "Actions visible to the caller (ADMIN: all, REQUESTER: created by them, REVIEWER: assigned to them), oldest first. state is a state name, e.g. PENDING.")
	@GetMapping
	PageResponse<ActionResponse> list(@RequestParam(required = false) String state,
			@RequestParam(required = false) ActionType type, @RequestParam(required = false) String assigneeId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, @CurrentUser User user) {
		return service.list(state, type, assigneeId, page, size, user);
	}

	@Operation(summary = "Action details")
	@ApiResponse(responseCode = "404", ref = OpenApiConfiguration.NOT_FOUND)
	@GetMapping("/{id}")
	ActionResponse get(@PathVariable long id, @CurrentUser User user) {
		return service.get(id, user);
	}

	@Operation(summary = "Transitions the caller can apply now")
	@ApiResponse(responseCode = "404", ref = OpenApiConfiguration.NOT_FOUND)
	@GetMapping("/{id}/transitions")
	List<AvailableTransition> availableTransitions(@PathVariable long id, @CurrentUser User user) {
		return service.availableTransitions(id, user);
	}

	@Operation(summary = "Apply a transition",
			description = "ASSIGN/REASSIGN need assigneeId (a reviewer); COMPLETE needs outcome (comment rules per type); REOPEN needs comment (the reason). Fields a transition does not use are ignored. Repeating COMPLETE with the same outcome returns 200 and changes nothing.")
	@ApiResponse(responseCode = "403", ref = OpenApiConfiguration.FORBIDDEN)
	@ApiResponse(responseCode = "404", ref = OpenApiConfiguration.NOT_FOUND)
	@ApiResponse(responseCode = "409", ref = OpenApiConfiguration.CONFLICT)
	@ApiResponse(responseCode = "422", ref = OpenApiConfiguration.UNPROCESSABLE)
	@PostMapping("/{id}/transitions")
	ActionResponse applyTransition(@PathVariable long id, @Valid @RequestBody TransitionCommand command,
			@CurrentUser User user) {
		return service.applyTransition(id, command, user);
	}

}
