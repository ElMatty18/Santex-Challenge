package io.github.elmatty18.workflowinbox.workflow;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;

/**
 * A handler whose transition may be repeated on an item that already reached its target state.
 * Only {@code COMPLETE} is idempotent: it is the human decision a client is most likely to retry.
 */
public interface IdempotentTransitionHandler extends TransitionHandler {

	/**
	 * @return {@code true} if applying the request again would not change the item
	 */
	boolean isRepeatOf(WorkflowItem item, TransitionRequest request);

	/**
	 * @return the error to report when the request is not a repeat of what was already applied
	 */
	ApiException repeatConflict(WorkflowItem item);

}
