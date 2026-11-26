package io.github.elmatty18.workflowinbox.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Selects the handler for a transition code; codes without a dedicated handler get the generic
 * one. Two handlers claiming the same code fail the application at startup.
 */
@Component
class TransitionHandlerRegistry {

	private static final TransitionHandler GENERIC = new GenericTransitionHandler();

	private final Map<String, TransitionHandler> handlersByCode = new HashMap<>();

	TransitionHandlerRegistry(List<TransitionHandler> handlers) {
		for (TransitionHandler handler : handlers) {
			for (String code : handler.codes()) {
				TransitionHandler previous = handlersByCode.putIfAbsent(code, handler);
				if (previous != null) {
					throw new IllegalStateException("Transition code %s is claimed by both %s and %s"
						.formatted(code, previous.getClass().getSimpleName(), handler.getClass().getSimpleName()));
				}
			}
		}
	}

	TransitionHandler handlerFor(String code) {
		return handlersByCode.getOrDefault(code, GENERIC);
	}

}
