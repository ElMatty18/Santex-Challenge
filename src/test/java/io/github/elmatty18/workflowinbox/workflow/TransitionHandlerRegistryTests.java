package io.github.elmatty18.workflowinbox.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class TransitionHandlerRegistryTests {

	private final AssignTransitionHandler assign = new AssignTransitionHandler(id -> Optional.empty());

	private final ReopenTransitionHandler reopen = new ReopenTransitionHandler();

	@Test
	void selectsHandlerByCode() {
		TransitionHandlerRegistry registry = new TransitionHandlerRegistry(List.of(assign, reopen));

		assertThat(registry.handlerFor("ASSIGN")).isSameAs(assign);
		assertThat(registry.handlerFor("REASSIGN")).isSameAs(assign);
		assertThat(registry.handlerFor("REOPEN")).isSameAs(reopen);
	}

	@Test
	void fallsBackToGenericHandlerForUnknownCode() {
		TransitionHandlerRegistry registry = new TransitionHandlerRegistry(List.of(assign, reopen));

		assertThat(registry.handlerFor("START_REVIEW")).isInstanceOf(GenericTransitionHandler.class);
	}

	@Test
	void rejectsTwoHandlersForTheSameCode() {
		TransitionHandler anotherReopen = new ReopenTransitionHandler();

		assertThatIllegalStateException().isThrownBy(() -> new TransitionHandlerRegistry(List.of(reopen, anotherReopen)))
			.withMessage("Transition code REOPEN is claimed by both ReopenTransitionHandler and ReopenTransitionHandler");
	}

}
