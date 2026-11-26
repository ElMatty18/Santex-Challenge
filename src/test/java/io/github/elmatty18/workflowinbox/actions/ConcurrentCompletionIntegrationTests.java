package io.github.elmatty18.workflowinbox.actions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * A real race: two users complete the same PENDING action with different outcomes at the same
 * moment. Which one wins, and how the loser learns about it, depends on timing:
 * <ul>
 * <li>both read version N, one commits first, the other's {@code UPDATE ... WHERE version = N}
 * matches no row → {@code 409 CONCURRENT_MODIFICATION};</li>
 * <li>the loser reads after the winner committed → it sees a COMPLETED action with another
 * outcome → {@code 409 ACTION_ALREADY_COMPLETED}.</li>
 * </ul>
 * Only timing-independent invariants are asserted. Every round uses a fresh action.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConcurrentCompletionIntegrationTests {

	private static final int ROUNDS = 25;

	private static final ExecutorService THREADS = Executors.newFixedThreadPool(2);

	@Autowired
	private MockMvcTester mvc;

	private ActionsApi api;

	@BeforeEach
	void setUp() {
		api = new ActionsApi(mvc);
	}

	@AfterAll
	static void shutDown() {
		THREADS.shutdownNow();
	}

	@Test
	void exactlyOneOfTwoSimultaneousCompletionsWins() throws Exception {
		Map<String, Integer> loserErrorCodes = new TreeMap<>();

		for (int round = 0; round < ROUNDS; round++) {
			long id = api.createAction("ana", ActionType.EXPENSE_APPROVAL, "lucia");
			long versionBefore = number(api.get("admin", "/api/v1/actions/{id}", id), "$.version");

			List<Attempt> attempts = race(id, new Attempt("lucia", "APPROVED", null),
					new Attempt("admin", "REJECTED", "Over budget"));

			List<Attempt> winners = attempts.stream().filter(a -> a.status == 200).toList();
			List<Attempt> losers = attempts.stream().filter(a -> a.status != 200).toList();
			assertThat(winners).as("round %d: exactly one 200", round).hasSize(1);
			assertThat(losers).as("round %d: the other is 409", round)
				.singleElement()
				.satisfies(loser -> {
					assertThat(loser.status).isEqualTo(409);
					assertThat(loser.errorCode).isIn("CONCURRENT_MODIFICATION", "ACTION_ALREADY_COMPLETED");
				});
			loserErrorCodes.merge(losers.getFirst().errorCode, 1, Integer::sum);

			Attempt winner = winners.getFirst();
			MvcTestResult action = api.get("admin", "/api/v1/actions/{id}", id);
			assertThat(ActionsApi.read(action, "$.state.name")).isEqualTo("COMPLETED");
			assertThat(ActionsApi.read(action, "$.outcome")).as("round %d: final outcome is the winner's", round)
				.isEqualTo(winner.outcome);
			assertThat(ActionsApi.read(action, "$.completedBy")).isEqualTo(winner.userId);
			assertThat(number(action, "$.version")).as("round %d: one version increment", round)
				.isEqualTo(versionBefore + 1);

			MvcTestResult audit = api.get("admin", "/api/v1/audit?size=100&actionId={id}", id);
			assertThat(listOf(audit, "$.items[?(@.field == 'state' && @.newValue == 'COMPLETED')].userId"))
				.as("round %d: one completion in the audit", round)
				.containsExactly(winner.userId);
			assertThat(listOf(audit, "$.items[?(@.field == 'outcome')].newValue")).containsExactly(winner.outcome);
		}

		// timing-dependent, so reported rather than asserted
		System.out.printf("Concurrent completion: %d rounds, loser error codes %s%n", ROUNDS, loserErrorCodes);
	}

	/**
	 * Sends both requests from two threads released together by a barrier.
	 */
	private List<Attempt> race(long actionId, Attempt first, Attempt second) throws Exception {
		CyclicBarrier start = new CyclicBarrier(2);
		Future<Attempt> a = THREADS.submit(() -> send(actionId, first, start));
		Future<Attempt> b = THREADS.submit(() -> send(actionId, second, start));
		return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
	}

	private Attempt send(long actionId, Attempt attempt, CyclicBarrier start) throws Exception {
		start.await(10, TimeUnit.SECONDS);
		MvcTestResult result = api.transition(attempt.userId, actionId, "COMPLETE", null, attempt.outcome,
				attempt.comment);
		int status = result.getResponse().getStatus();
		attempt.status = status;
		attempt.errorCode = status == 200 ? null : (String) ActionsApi.read(result, "$.errorCode");
		return attempt;
	}

	private static long number(MvcTestResult result, String jsonPath) {
		return ((Number) ActionsApi.read(result, jsonPath)).longValue();
	}

	@SuppressWarnings("unchecked")
	private static List<Object> listOf(MvcTestResult result, String jsonPath) {
		return (List<Object>) ActionsApi.read(result, jsonPath);
	}

	/** One side of the race: who, with what, and what came back. */
	private static final class Attempt {

		final String userId;

		final String outcome;

		final String comment;

		int status;

		String errorCode;

		Attempt(String userId, String outcome, String comment) {
			this.userId = userId;
			this.outcome = outcome;
			this.comment = comment;
		}

	}

}
