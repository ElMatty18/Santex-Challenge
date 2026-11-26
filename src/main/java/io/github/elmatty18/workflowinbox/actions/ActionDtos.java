package io.github.elmatty18.workflowinbox.actions;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.github.elmatty18.workflowinbox.workflow.Outcome;
import io.github.elmatty18.workflowinbox.workflow.TransitionRequest;
import io.github.elmatty18.workflowinbox.workflow.WorkflowState;

/**
 * Request and response bodies of the actions API.
 */
final class ActionDtos {

	private ActionDtos() {
	}

	record CreateActionRequest(@NotNull ActionType type, @NotBlank @Size(max = 120) String title,
			@Size(max = 2000) String description, String assigneeId) {

	}

	record TransitionCommand(@NotBlank String transition, String assigneeId, Outcome outcome,
			@Size(max = 2000) String comment) {

		TransitionRequest toRequest() {
			return new TransitionRequest(transition, assigneeId, outcome, comment);
		}

	}

	record StateRef(int code, String name) {

		static StateRef of(WorkflowState state) {
			return new StateRef(state.code(), state.name());
		}

	}

	record ActionResponse(Long id, ActionType type, String title, String description, StateRef state,
			String requesterId, String assigneeId, Outcome outcome, String comment, String completedBy,
			Instant completedAt, Instant createdAt, Instant updatedAt, Long version) {

		static ActionResponse of(Action action, WorkflowState state) {
			return new ActionResponse(action.id(), action.type(), action.title(), action.description(),
					StateRef.of(state), action.requesterId(), action.assigneeId(), action.outcome(), action.comment(),
					action.completedBy(), action.completedAt(), action.createdAt(), action.updatedAt(),
					action.version());
		}

	}

}
