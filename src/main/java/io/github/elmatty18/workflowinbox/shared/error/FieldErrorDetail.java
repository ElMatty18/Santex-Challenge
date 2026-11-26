package io.github.elmatty18.workflowinbox.shared.error;

/**
 * One entry of the {@code fieldErrors} list in a {@code VALIDATION_FAILED} response.
 */
public record FieldErrorDetail(String field, String message) {

}
