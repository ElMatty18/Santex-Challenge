package io.github.elmatty18.workflowinbox.shared.web;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import org.springdoc.core.customizers.OpenApiCustomizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API metadata and the Problem Details error responses. Every operation gets the errors any
 * request can produce (400, 401, 500); controllers reference the specific ones (403, 404, 409,
 * 422) by name, e.g. {@code @ApiResponse(responseCode = "404", ref = NOT_FOUND)}.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

	public static final String FORBIDDEN = "#/components/responses/Forbidden";

	public static final String NOT_FOUND = "#/components/responses/NotFound";

	public static final String CONFLICT = "#/components/responses/Conflict";

	public static final String UNPROCESSABLE = "#/components/responses/UnprocessableContent";

	private static final String PROBLEM_JSON = "application/problem+json";

	/** Responses added to every operation, by status code. */
	private static final Map<String, String> COMMON_RESPONSES = Map.of("400", "BadRequest", "401", "Unauthorized",
			"500", "InternalError");

	@Bean
	OpenAPI openApi() {
		Components components = new Components().addSchemas("FieldError", fieldErrorSchema())
			.addSchemas("Problem", problemSchema());
		errorResponses().forEach(components::addResponses);
		return new OpenAPI().components(components)
			.info(new Info().title("Workflow Inbox API")
				.version("v1")
				.description("Internal operations inbox: actions that need a human decision, moved through a "
						+ "data-driven workflow, with a field-level audit log. Every /api request is identified by "
						+ "the X-User-Id header (no real authentication). Seeded users: admin, ana (requester), "
						+ "lucia and martin (reviewers)."));
	}

	@Bean
	OpenApiCustomizer commonErrorResponses() {
		return openApi -> openApi.getPaths()
			.values()
			.forEach(path -> path.readOperations().forEach(operation -> {
				ApiResponses responses = operation.getResponses();
				COMMON_RESPONSES.forEach((status, name) -> {
					if (!responses.containsKey(status)) {
						responses.addApiResponse(status, new ApiResponse().$ref("#/components/responses/" + name));
					}
				});
			}));
	}

	private static Map<String, ApiResponse> errorResponses() {
		Map<String, ApiResponse> responses = new LinkedHashMap<>();
		responses.put("BadRequest", problem("VALIDATION_FAILED: invalid body, parameter or JSON (see fieldErrors)"));
		responses.put("Unauthorized", problem("UNAUTHENTICATED: missing or unknown X-User-Id"));
		responses.put("Forbidden", problem("FORBIDDEN: the caller's role may not perform this operation"));
		responses.put("NotFound",
				problem("ACTION_NOT_FOUND: the action does not exist or is not visible to the caller"));
		responses.put("Conflict", problem("INVALID_TRANSITION (not allowed from the current state), "
				+ "ACTION_ALREADY_COMPLETED (different outcome; includes outcome, completedBy, completedAt) or "
				+ "CONCURRENT_MODIFICATION (another request changed the action)"));
		responses.put("UnprocessableContent",
				problem("BUSINESS_RULE_VIOLATION: outcome invalid for the type, missing comment or reason, "
						+ "assignee not a reviewer"));
		responses.put("InternalError", problem("INTERNAL_ERROR: unexpected error; quote the requestId"));
		return responses;
	}

	private static ApiResponse problem(String description) {
		Schema<?> ref = new Schema<>().$ref("#/components/schemas/Problem");
		return new ApiResponse().description(description)
			.content(new Content().addMediaType(PROBLEM_JSON, new MediaType().schema(ref)));
	}

	private static Schema<?> problemSchema() {
		List<String> errorCodes = Arrays.stream(ErrorCode.values()).map(Enum::name).toList();
		return new ObjectSchema().description("RFC 9457 Problem Details")
			.addProperty("type", new StringSchema().example("about:blank"))
			.addProperty("title", new StringSchema().example("Not Found"))
			.addProperty("status", new IntegerSchema().example(404))
			.addProperty("detail", new StringSchema().example("Action 42 not found"))
			.addProperty("instance", new StringSchema().example("/api/v1/actions/42"))
			.addProperty("errorCode", new StringSchema()._enum(errorCodes).example("ACTION_NOT_FOUND"))
			.addProperty("requestId",
					new StringSchema().description("Same value as the X-Request-Id response header"))
			.addProperty("fieldErrors", new ArraySchema().description("Only for VALIDATION_FAILED")
				.items(new Schema<>().$ref("#/components/schemas/FieldError")));
	}

	private static Schema<?> fieldErrorSchema() {
		return new ObjectSchema().addProperty("field", new StringSchema().example("title"))
			.addProperty("message", new StringSchema().example("must not be blank"));
	}

}
