package io.github.elmatty18.workflowinbox.actions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Metrics read through /actuator/metrics, as an operator would. Meters are cumulative across
 * tests, so every assertion is on the increase caused by the test itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransitionMetricsIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	private ActionsApi api;

	@BeforeEach
	void setUp() {
		api = new ActionsApi(mvc);
	}

	@Test
	void countsAndTimesSuccessfulTransitions() {
		long id = api.createAction("ana", ActionType.ONBOARDING, "lucia");
		double successBefore = count("inbox.transitions", "transition:COMPLETE", "result:success");
		double timedBefore = count("inbox.transition.duration", "transition:COMPLETE");

		assertThat(api.transition("lucia", id, "COMPLETE", null, "DONE", null)).hasStatusOk();

		assertThat(count("inbox.transitions", "transition:COMPLETE", "result:success")).isEqualTo(successBefore + 1);
		assertThat(count("inbox.transition.duration", "transition:COMPLETE")).isEqualTo(timedBefore + 1);
	}

	@Test
	void assignOnCreateIsCountedAsATransition() {
		double before = count("inbox.transitions", "transition:ASSIGN", "result:success");

		api.createAction("ana", ActionType.ONBOARDING, "lucia");

		assertThat(count("inbox.transitions", "transition:ASSIGN", "result:success")).isEqualTo(before + 1);
	}

	@Test
	void countsRejectedTransitions() {
		long id = api.createAction("ana", ActionType.DOCUMENT_UPLOAD, "lucia");
		double before = count("inbox.transitions", "transition:COMPLETE", "result:rejected");

		assertThat(api.transition("lucia", id, "COMPLETE", null, "APPROVED", null))
			.hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(api.transition("martin", id, "COMPLETE", null, "DONE", null)).hasStatus(HttpStatus.NOT_FOUND);

		assertThat(count("inbox.transitions", "transition:COMPLETE", "result:rejected")).isEqualTo(before + 2);
	}

	@Test
	void unconfiguredTransitionCodesShareOneTagValue() {
		long id = api.createAction("ana", ActionType.ONBOARDING, null);
		String randomCode = "HACK_" + UUID.randomUUID();
		double before = count("inbox.transitions", "transition:UNKNOWN", "result:rejected");

		assertThat(api.transition("admin", id, randomCode, null, null, null)).hasStatus(HttpStatus.CONFLICT);

		assertThat(count("inbox.transitions", "transition:UNKNOWN", "result:rejected")).isEqualTo(before + 1);
		List<String> transitionTagValues = metricRead("/actuator/metrics/inbox.transitions",
				"$.availableTags[?(@.tag == 'transition')].values[*]");
		assertThat(transitionTagValues).doesNotContain(randomCode).allMatch(value -> value.matches("[A-Z_]+"));
	}

	/**
	 * @return the COUNT statistic of the meter with these tags, or 0 if it was never recorded
	 */
	private double count(String metric, String... tags) {
		var request = mvc.get().uri("/actuator/metrics/" + metric);
		for (String tag : tags) {
			request = request.queryParam("tag", tag);
		}
		MvcTestResult result = request.exchange();
		if (result.getResponse().getStatus() == HttpStatus.NOT_FOUND.value()) {
			return 0;
		}
		List<Number> counts = metricValues(result);
		return counts.isEmpty() ? 0 : counts.getFirst().doubleValue();
	}

	@SuppressWarnings("unchecked")
	private static List<Number> metricValues(MvcTestResult result) {
		return (List<Number>) ActionsApi.read(result, "$.measurements[?(@.statistic == 'COUNT')].value");
	}

	@SuppressWarnings("unchecked")
	private <T> T metricRead(String uri, String jsonPath) {
		return (T) ActionsApi.read(mvc.get().uri(uri).exchange(), jsonPath);
	}

}
