package io.github.elmatty18.workflowinbox.workflow;

/**
 * @param applied {@code false} for an idempotent repeat: the item was not modified, so the caller
 * must not save it or publish a change event
 */
public record TransitionResult(String code, WorkflowState from, WorkflowState to, boolean applied) {

}
