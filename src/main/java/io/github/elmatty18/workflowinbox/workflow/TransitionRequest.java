package io.github.elmatty18.workflowinbox.workflow;

/**
 * Input to {@link TransitionEngine#apply}. Fields a transition does not use are ignored. Blank
 * {@code assigneeId} and {@code comment} are normalized to {@code null}.
 */
public record TransitionRequest(String code, String assigneeId, Outcome outcome, String comment) {

	public TransitionRequest {
		assigneeId = blankToNull(assigneeId);
		comment = blankToNull(comment);
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

}
