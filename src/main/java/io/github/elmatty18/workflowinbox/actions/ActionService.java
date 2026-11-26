package io.github.elmatty18.workflowinbox.actions;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.github.elmatty18.workflowinbox.actions.ActionDtos.ActionResponse;
import io.github.elmatty18.workflowinbox.actions.ActionDtos.CreateActionRequest;
import io.github.elmatty18.workflowinbox.actions.ActionDtos.TransitionCommand;
import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;
import io.github.elmatty18.workflowinbox.shared.events.ActionChangedEvent;
import io.github.elmatty18.workflowinbox.shared.events.FieldChange;
import io.github.elmatty18.workflowinbox.shared.web.PageResponse;
import io.github.elmatty18.workflowinbox.users.Role;
import io.github.elmatty18.workflowinbox.users.User;
import io.github.elmatty18.workflowinbox.workflow.AvailableTransition;
import io.github.elmatty18.workflowinbox.workflow.TransitionEngine;
import io.github.elmatty18.workflowinbox.workflow.TransitionRequest;
import io.github.elmatty18.workflowinbox.workflow.TransitionResult;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Action use cases. Every change goes through the transition engine and publishes an
 * {@link ActionChangedEvent} in the same transaction.
 */
@Service
class ActionService {

	private static final String ASSIGN = "ASSIGN";

	private static final Sort OLDEST_FIRST = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

	private final ActionRepository repository;

	private final TransitionEngine engine;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	ActionService(ActionRepository repository, TransitionEngine engine, ApplicationEventPublisher events, Clock clock) {
		this.repository = repository;
		this.engine = engine;
		this.events = events;
		this.clock = clock;
	}

	/**
	 * Creates the action in the initial state and, if an assignee is given, applies {@code ASSIGN}
	 * in the same transaction: a failed assignment leaves no action behind.
	 */
	@Transactional
	ActionResponse create(CreateActionRequest request, User user) {
		if (!user.hasRole(Role.REQUESTER) && !user.hasRole(Role.ADMIN)) {
			throw new ApiException(ErrorCode.FORBIDDEN, "Role %s may not create actions".formatted(user.role()));
		}
		Instant now = clock.instant();
		Action action = repository.save(Action.create(request.type(), request.title(), request.description(), user.id(),
				engine.initialState().code(), now));
		publish(action, user, ActionSnapshot.NONE.changesTo(snapshot(action)), now);

		if (request.assigneeId() != null && !request.assigneeId().isBlank()) {
			applyTransition(action, user, new TransitionRequest(ASSIGN, request.assigneeId(), null, null));
		}
		return toResponse(action);
	}

	@Transactional(readOnly = true)
	ActionResponse get(long id, User user) {
		return toResponse(findVisible(id, user));
	}

	@Transactional(readOnly = true)
	List<AvailableTransition> availableTransitions(long id, User user) {
		return engine.availableTransitions(findVisible(id, user), user);
	}

	/**
	 * Inbox: actions visible to the user, optionally filtered, oldest first.
	 */
	@Transactional(readOnly = true)
	PageResponse<ActionResponse> list(String stateName, ActionType type, String assigneeId, int page, int size,
			User user) {
		List<Specification<Action>> conditions = new ArrayList<>();
		conditions.add(ActionSpecifications.visibleTo(user));
		if (stateName != null) {
			int stateCode = engine.findStateByName(stateName)
				.orElseThrow(() -> ApiException.validation("state", "Unknown state '%s'".formatted(stateName)))
				.code();
			conditions.add(ActionSpecifications.inState(stateCode));
		}
		if (type != null) {
			conditions.add(ActionSpecifications.ofType(type));
		}
		if (assigneeId != null) {
			conditions.add(ActionSpecifications.assignedTo(assigneeId));
		}
		return PageResponse.from(
				repository.findAll(Specification.allOf(conditions), PageRequest.of(page, size, OLDEST_FIRST)),
				this::toResponse);
	}

	/**
	 * Visibility is checked first (404), then the engine applies its own checks (403, 409, 422).
	 * An idempotent repeat returns the action unchanged: nothing is saved or published.
	 */
	@Transactional
	ActionResponse applyTransition(long id, TransitionCommand command, User user) {
		Action action = findVisible(id, user);
		applyTransition(action, user, command.toRequest());
		return toResponse(action);
	}

	private void applyTransition(Action action, User user, TransitionRequest request) {
		// one instant per change: completedAt, updatedAt and the audit rows all share it
		Instant now = clock.instant();
		ActionSnapshot before = snapshot(action);
		TransitionResult result = engine.apply(action, user, request, now);
		if (!result.applied()) {
			return;
		}
		action.touch(now);
		// flush now so an optimistic locking conflict surfaces here, before the event is published
		repository.saveAndFlush(action);
		publish(action, user, before.changesTo(snapshot(action)), now);
	}

	private Action findVisible(long id, User user) {
		return repository.findOne(ActionSpecifications.visibleTo(user).and(ActionSpecifications.hasId(id)))
			.orElseThrow(() -> new ApiException(ErrorCode.ACTION_NOT_FOUND, "Action %d not found".formatted(id)));
	}

	private void publish(Action action, User user, List<FieldChange> changes, Instant now) {
		events.publishEvent(new ActionChangedEvent(action.id(), user.id(), changes, now));
	}

	private ActionSnapshot snapshot(Action action) {
		return ActionSnapshot.of(action, engine.state(action.stateCode()).name());
	}

	private ActionResponse toResponse(Action action) {
		return ActionResponse.of(action, engine.state(action.stateCode()));
	}

}
