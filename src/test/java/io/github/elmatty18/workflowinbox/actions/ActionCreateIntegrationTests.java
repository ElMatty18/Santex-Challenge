package io.github.elmatty18.workflowinbox.actions;

import static io.github.elmatty18.workflowinbox.actions.ActionsApi.ISO_UTC;
import static io.github.elmatty18.workflowinbox.actions.ActionsApi.createBody;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.github.elmatty18.workflowinbox.shared.events.ActionChangedEvent;
import io.github.elmatty18.workflowinbox.shared.events.FieldChange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Every test creates its own actions; seeded data is never modified.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class ActionCreateIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ApplicationEvents events;

	private ActionsApi api;

	@BeforeEach
	void setUp() {
		api = new ActionsApi(mvc);
	}

	@Test
	void requesterCreatesActionInInitialState() {
		MvcTestResult result = api.create("ana", createBody(ActionType.EXPENSE_APPROVAL, "Taxi to airport", null));

		long id = ((Number) ActionsApi.read(result, "$.id")).longValue();
		assertThat(result).hasStatus(HttpStatus.CREATED)
			.hasHeader("Location", "http://localhost/api/v1/actions/" + id)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
		assertThat(result).bodyJson().extractingPath("$.state.name").isEqualTo("CREATED");
		assertThat(result).bodyJson().extractingPath("$.state.code").isEqualTo(0);
		assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Taxi to airport");
		assertThat(result).bodyJson().extractingPath("$.requesterId").isEqualTo("ana");
		assertThat(result).bodyJson().extractingPath("$.assigneeId").isNull();
		assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(0);
		assertThat(result).bodyJson().extractingPath("$.createdAt").asString().matches(ISO_UTC);
		assertThat(result).bodyJson().extractingPath("$.updatedAt").asString().matches(ISO_UTC);

		assertThat(events.stream(ActionChangedEvent.class)).singleElement().satisfies(event -> {
			assertThat(event.actionId()).isEqualTo(id);
			assertThat(event.userId()).isEqualTo("ana");
			assertThat(event.changes()).containsExactly(new FieldChange("state", null, "CREATED"));
		});
	}

	@Test
	void createWithAssigneeAppliesAssignInTheSameRequest() {
		MvcTestResult result = api.create("ana", createBody(ActionType.DEPLOYMENT_REVIEW, "Deploy api v3", "lucia"));

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.state.name").isEqualTo("PENDING");
		assertThat(result).bodyJson().extractingPath("$.assigneeId").isEqualTo("lucia");

		// same rows as the example in SPEC section 11
		assertThat(events.stream(ActionChangedEvent.class).map(ActionChangedEvent::changes)).containsExactly(
				List.of(new FieldChange("state", null, "CREATED")),
				List.of(new FieldChange("assignee", null, "lucia"), new FieldChange("state", "CREATED", "PENDING")));
	}

	@Test
	void adminCanCreate() {
		assertThat(api.create("admin", createBody(ActionType.ONBOARDING, "Onboard Bob", null)))
			.hasStatus(HttpStatus.CREATED);
	}

	@Test
	void reviewerCannotCreate() {
		assertThat(api.create("lucia", createBody(ActionType.ONBOARDING, "Onboard Bob", null)))
			.hasStatus(HttpStatus.FORBIDDEN)
			.bodyJson()
			.extractingPath("$.errorCode")
			.isEqualTo("FORBIDDEN");
	}

	@Test
	void invalidAssigneeRollsBackTheWholeCreation() {
		long before = totalActions();

		MvcTestResult result = api.create("ana", createBody(ActionType.ONBOARDING, "Onboard Bob", "admin"));

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
			.bodyJson()
			.extractingPath("$.errorCode")
			.isEqualTo("BUSINESS_RULE_VIOLATION");
		assertThat(totalActions()).isEqualTo(before);
	}

	@Test
	void rejectsBlankTitle() {
		assertValidationError(api.create("ana", createBody(ActionType.ONBOARDING, "  ", null)), "title");
	}

	@Test
	void rejectsTitleLongerThan120Characters() {
		assertValidationError(api.create("ana", createBody(ActionType.ONBOARDING, "x".repeat(121), null)), "title");
	}

	@Test
	void acceptsTitleOf120Characters() {
		assertThat(api.create("ana", createBody(ActionType.ONBOARDING, "x".repeat(120), null)))
			.hasStatus(HttpStatus.CREATED);
	}

	@Test
	void rejectsMissingType() {
		assertValidationError(api.create("ana", createBody(null, "No type", null)), "type");
	}

	@Test
	void rejectsUnknownType() {
		assertValidationError(api.create("ana", """
				{"type": "VACATION", "title": "Two weeks off"}"""), "type");
	}

	@Test
	void rejectsMalformedJson() {
		assertThat(api.create("ana", "{\"type\": ")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errorCode")
			.isEqualTo("VALIDATION_FAILED");
	}

	private long totalActions() {
		return ((Number) ActionsApi.read(api.get("admin", "/api/v1/actions?size=1"), "$.totalElements")).longValue();
	}

	private static void assertValidationError(MvcTestResult result, String field) {
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
		assertThat(result).bodyJson().extractingPath("$.fieldErrors[*].field").asArray().containsExactly(field);
	}

}
