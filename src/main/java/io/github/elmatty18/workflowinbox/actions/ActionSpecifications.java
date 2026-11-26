package io.github.elmatty18.workflowinbox.actions;

import io.github.elmatty18.workflowinbox.users.User;

import org.springframework.data.jpa.domain.Specification;

/**
 * Query building blocks. {@link #visibleTo} is the single definition of visibility, used both for
 * the inbox and for single-action lookups (404 when not visible).
 */
final class ActionSpecifications {

	private ActionSpecifications() {
	}

	static Specification<Action> visibleTo(User user) {
		return switch (user.role()) {
			case ADMIN -> (root, query, cb) -> cb.conjunction();
			case REQUESTER -> (root, query, cb) -> cb.equal(root.get("requesterId"), user.id());
			case REVIEWER -> (root, query, cb) -> cb.equal(root.get("assigneeId"), user.id());
		};
	}

	static Specification<Action> hasId(long id) {
		return (root, query, cb) -> cb.equal(root.get("id"), id);
	}

	static Specification<Action> inState(int stateCode) {
		return (root, query, cb) -> cb.equal(root.get("stateCode"), stateCode);
	}

	static Specification<Action> ofType(ActionType type) {
		return (root, query, cb) -> cb.equal(root.get("type"), type);
	}

	static Specification<Action> assignedTo(String assigneeId) {
		return (root, query, cb) -> cb.equal(root.get("assigneeId"), assigneeId);
	}

}
