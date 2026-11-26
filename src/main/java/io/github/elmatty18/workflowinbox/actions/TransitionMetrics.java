package io.github.elmatty18.workflowinbox.actions;

import java.util.function.Supplier;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.workflow.TransitionEngine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * {@code inbox.transitions} (tags {@code transition}, {@code result}) and
 * {@code inbox.transition.duration} (tag {@code transition}).
 * <p>
 * Tags are kept low-cardinality: {@code transition} is a configured code or {@code UNKNOWN} (the
 * code comes from the request body, so any string could arrive), and {@code result} is
 * {@code success}, {@code rejected} (an expected 4xx outcome) or {@code error} (unexpected).
 */
@Component
class TransitionMetrics {

	static final String UNKNOWN = "UNKNOWN";

	private final MeterRegistry registry;

	private final TransitionEngine engine;

	TransitionMetrics(MeterRegistry registry, TransitionEngine engine) {
		this.registry = registry;
		this.engine = engine;
	}

	<T> T record(String transitionCode, Supplier<T> transition) {
		String tag = engine.isConfiguredTransition(transitionCode) ? transitionCode : UNKNOWN;
		Timer.Sample sample = Timer.start(registry);
		String result = "error";
		try {
			T value = transition.get();
			result = "success";
			return value;
		}
		catch (ApiException | OptimisticLockingFailureException ex) {
			result = "rejected";
			throw ex;
		}
		finally {
			sample.stop(Timer.builder("inbox.transition.duration")
				.description("Time to apply a workflow transition")
				.tag("transition", tag)
				.register(registry));
			Counter.builder("inbox.transitions")
				.description("Workflow transitions requested, by outcome")
				.tag("transition", tag)
				.tag("result", result)
				.register(registry)
				.increment();
		}
	}

}
