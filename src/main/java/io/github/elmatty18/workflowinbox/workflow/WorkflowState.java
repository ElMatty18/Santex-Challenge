package io.github.elmatty18.workflowinbox.workflow;

/**
 * A configured workflow state. {@code code} is an identifier with gaps, never compared to infer
 * order.
 */
public record WorkflowState(int code, String name, boolean initial, boolean finalState) {

}
