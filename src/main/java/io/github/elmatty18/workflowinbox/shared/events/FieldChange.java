package io.github.elmatty18.workflowinbox.shared.events;

/**
 * One audited field that changed. Values are text; {@code null} means empty.
 */
public record FieldChange(String field, String oldValue, String newValue) {

}
