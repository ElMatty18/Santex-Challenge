package io.github.elmatty18.workflowinbox.workflow;

import java.time.Instant;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;

/**
 * In-memory {@link WorkflowItem} whose completion policy mimics EXPENSE_APPROVAL: APPROVED or
 * REJECTED, comment required when REJECTED.
 */
class FakeWorkflowItem implements WorkflowItem {

	static final CompletionPolicy APPROVE_OR_REJECT = (outcome, comment) -> {
		if (outcome != Outcome.APPROVED && outcome != Outcome.REJECTED) {
			throw new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, "Outcome " + outcome + " is not valid");
		}
		if (outcome == Outcome.REJECTED && comment == null) {
			throw new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, "comment is required when REJECTED");
		}
	};

	private int stateCode;

	private String assigneeId;

	private Outcome outcome;

	private String comment;

	private String completedBy;

	private Instant completedAt;

	FakeWorkflowItem(int stateCode) {
		this.stateCode = stateCode;
	}

	static FakeWorkflowItem created() {
		return new FakeWorkflowItem(TestWorkflows.CREATED);
	}

	static FakeWorkflowItem pendingFor(String assigneeId) {
		FakeWorkflowItem item = new FakeWorkflowItem(TestWorkflows.PENDING);
		item.assigneeId = assigneeId;
		return item;
	}

	static FakeWorkflowItem completedBy(String assigneeId, Outcome outcome, String comment, Instant at) {
		FakeWorkflowItem item = pendingFor(assigneeId);
		item.complete(outcome, comment, assigneeId, at);
		item.moveTo(TestWorkflows.COMPLETED);
		return item;
	}

	/**
	 * @return a copy, to assert that an operation left the item untouched
	 */
	FakeWorkflowItem copy() {
		FakeWorkflowItem copy = new FakeWorkflowItem(stateCode);
		copy.assigneeId = assigneeId;
		copy.outcome = outcome;
		copy.comment = comment;
		copy.completedBy = completedBy;
		copy.completedAt = completedAt;
		return copy;
	}

	@Override
	public int stateCode() {
		return stateCode;
	}

	@Override
	public String assigneeId() {
		return assigneeId;
	}

	@Override
	public Outcome outcome() {
		return outcome;
	}

	@Override
	public String comment() {
		return comment;
	}

	@Override
	public String completedBy() {
		return completedBy;
	}

	@Override
	public Instant completedAt() {
		return completedAt;
	}

	@Override
	public CompletionPolicy completionPolicy() {
		return APPROVE_OR_REJECT;
	}

	@Override
	public void moveTo(int stateCode) {
		this.stateCode = stateCode;
	}

	@Override
	public void assignTo(String assigneeId) {
		this.assigneeId = assigneeId;
	}

	@Override
	public void complete(Outcome outcome, String comment, String completedBy, Instant completedAt) {
		this.outcome = outcome;
		this.comment = comment;
		this.completedBy = completedBy;
		this.completedAt = completedAt;
	}

	@Override
	public void reopen(String reason) {
		this.comment = reason;
		this.outcome = null;
		this.completedBy = null;
		this.completedAt = null;
	}

}
