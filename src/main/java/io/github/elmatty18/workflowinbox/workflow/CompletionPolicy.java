package io.github.elmatty18.workflowinbox.workflow;

/**
 * Which outcomes (and comments) are valid when completing an item. Implemented by the actions
 * module (per action type), applied by the {@code COMPLETE} handler, so the workflow module does
 * not depend on action types.
 */
public interface CompletionPolicy {

	/**
	 * @param comment may be {@code null}; blank comments are passed as {@code null}
	 * @throws io.github.elmatty18.workflowinbox.shared.error.ApiException with
	 * {@code BUSINESS_RULE_VIOLATION} if the outcome or comment is not acceptable
	 */
	void validate(Outcome outcome, String comment);

}
