package io.github.elmatty18.workflowinbox.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTests {

	private static final String UUID = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

	private final RequestIdFilter filter = new RequestIdFilter();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	/** MDC as seen by the rest of the chain. */
	private final Map<String, String> mdcDuringRequest = new HashMap<>();

	private final MockFilterChain chain = new MockFilterChain() {
		@Override
		public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
			mdcDuringRequest.putAll(MDC.getCopyOfContextMap());
		}
	};

	@Test
	void keepsCallerRequestIdAndExposesItInMdcAndResponse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Request-Id", "checkout-42.retry:1");
		request.addHeader("X-User-Id", "ana");

		filter.doFilter(request, response, chain);

		assertThat(response.getHeader("X-Request-Id")).isEqualTo("checkout-42.retry:1");
		assertThat(mdcDuringRequest).containsEntry("requestId", "checkout-42.retry:1").containsEntry("userId", "ana");
	}

	@Test
	void generatesRequestIdWhenMissing() throws Exception {
		filter.doFilter(new MockHttpServletRequest(), response, chain);

		assertThat(response.getHeader("X-Request-Id")).matches(UUID);
		assertThat(mdcDuringRequest.get("requestId")).isEqualTo(response.getHeader("X-Request-Id"));
		assertThat(mdcDuringRequest).doesNotContainKey("userId");
	}

	@Test
	void replacesUnsafeRequestIdAndIgnoresUnsafeUserId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Request-Id", "id\nINFO forged log line");
		request.addHeader("X-User-Id", "ana\nadmin");

		filter.doFilter(request, response, chain);

		assertThat(response.getHeader("X-Request-Id")).matches(UUID);
		assertThat(mdcDuringRequest).doesNotContainKey("userId");
	}

	@Test
	void replacesRequestIdLongerThanTheAuditColumn() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Request-Id", "x".repeat(101));

		filter.doFilter(request, response, chain);

		assertThat(response.getHeader("X-Request-Id")).matches(UUID);
	}

	@Test
	void clearsMdcAfterTheRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User-Id", "ana");

		filter.doFilter(request, response, chain);

		assertThat(MDC.get("requestId")).isNull();
		assertThat(MDC.get("userId")).isNull();
	}

}
