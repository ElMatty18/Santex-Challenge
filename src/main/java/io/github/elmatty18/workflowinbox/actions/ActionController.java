package io.github.elmatty18.workflowinbox.actions;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.github.elmatty18.workflowinbox.actions.ActionDtos.ActionResponse;
import io.github.elmatty18.workflowinbox.actions.ActionDtos.CreateActionRequest;
import io.github.elmatty18.workflowinbox.actions.ActionDtos.TransitionCommand;
import io.github.elmatty18.workflowinbox.shared.web.PageResponse;
import io.github.elmatty18.workflowinbox.users.CurrentUser;
import io.github.elmatty18.workflowinbox.users.User;
import io.github.elmatty18.workflowinbox.workflow.AvailableTransition;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/actions")
class ActionController {

	private final ActionService service;

	ActionController(ActionService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ActionResponse> create(@Valid @RequestBody CreateActionRequest request, @CurrentUser User user) {
		ActionResponse created = service.create(request, user);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@GetMapping
	PageResponse<ActionResponse> list(@RequestParam(required = false) String state,
			@RequestParam(required = false) ActionType type, @RequestParam(required = false) String assigneeId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, @CurrentUser User user) {
		return service.list(state, type, assigneeId, page, size, user);
	}

	@GetMapping("/{id}")
	ActionResponse get(@PathVariable long id, @CurrentUser User user) {
		return service.get(id, user);
	}

	@GetMapping("/{id}/transitions")
	List<AvailableTransition> availableTransitions(@PathVariable long id, @CurrentUser User user) {
		return service.availableTransitions(id, user);
	}

	@PostMapping("/{id}/transitions")
	ActionResponse applyTransition(@PathVariable long id, @Valid @RequestBody TransitionCommand command,
			@CurrentUser User user) {
		return service.applyTransition(id, command, user);
	}

}
