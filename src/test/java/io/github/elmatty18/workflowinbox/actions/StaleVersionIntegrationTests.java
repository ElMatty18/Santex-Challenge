package io.github.elmatty18.workflowinbox.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.List;

import io.github.elmatty18.workflowinbox.workflow.TransitionEngine;
import io.github.elmatty18.workflowinbox.workflow.WorkflowItem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Deterministic version of the race: while a COMPLETE request is in flight (action loaded and
 * validated, not yet written), another transaction commits a change to the same action. The
 * request's write then carries a stale version and must become 409 CONCURRENT_MODIFICATION.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class StaleVersionIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@MockitoSpyBean
	private TransitionEngine engine;

	@Test
	void staleVersionOnWriteIsConcurrentModification(CapturedOutput output) {
		ActionsApi api = new ActionsApi(mvc);
		long id = api.createAction("ana", ActionType.EXPENSE_APPROVAL, "lucia");
		long versionBefore = version(api.get("admin", "/api/v1/actions/{id}", id));

		// after the engine applied the transition in memory, another transaction commits first
		TransactionTemplate otherTransaction = new TransactionTemplate(transactionManager);
		otherTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		doAnswer(invocation -> {
			Object result = invocation.callRealMethod();
			otherTransaction.executeWithoutResult(
					status -> jdbc.update("UPDATE action SET version = version + 1 WHERE id = ?", id));
			return result;
		}).when(engine).apply(any(WorkflowItem.class), any(), any(), any());

		MvcTestResult result = api.transition("lucia", id, "COMPLETE", null, "APPROVED", null);

		assertThat(result).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.errorCode")
			.isEqualTo("CONCURRENT_MODIFICATION");
		// the request's change was rolled back: only the other transaction's increment remains
		MvcTestResult action = api.get("admin", "/api/v1/actions/{id}", id);
		assertThat(ActionsApi.read(action, "$.state.name")).isEqualTo("PENDING");
		assertThat(ActionsApi.read(action, "$.outcome")).isNull();
		assertThat(version(action)).isEqualTo(versionBefore + 1);
		assertThat(completionRows(api, id)).isEmpty();
		// logged by entity and id only, never with the SQL statement from the exception message
		assertThat(output).contains("Request rejected: CONCURRENT_MODIFICATION on Action " + id)
			.doesNotContainIgnoringCase("update action set");
	}

	private static long version(MvcTestResult action) {
		return ((Number) ActionsApi.read(action, "$.version")).longValue();
	}

	@SuppressWarnings("unchecked")
	private static List<Object> completionRows(ActionsApi api, long id) {
		return (List<Object>) ActionsApi.read(api.get("admin", "/api/v1/audit?size=100&actionId={id}", id),
				"$.items[?(@.field == 'state' && @.newValue == 'COMPLETED')]");
	}

}
