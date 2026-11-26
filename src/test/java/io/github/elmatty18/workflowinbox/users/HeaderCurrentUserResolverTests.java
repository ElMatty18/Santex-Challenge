package io.github.elmatty18.workflowinbox.users;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

class HeaderCurrentUserResolverTests {

	private final User ana = new User("ana", "Ana Requester", Role.REQUESTER);

	private final UserDirectory directory = id -> Optional.ofNullable(Map.of("ana", ana).get(id));

	private final HeaderCurrentUserResolver resolver = new HeaderCurrentUserResolver(directory);

	@Test
	void resolvesKnownUser() {
		assertThat(resolver.resolve(requestWithUser("ana"))).contains(ana);
	}

	@Test
	void ignoresSurroundingWhitespace() {
		assertThat(resolver.resolve(requestWithUser("  ana "))).contains(ana);
	}

	@Test
	void emptyWhenHeaderMissing() {
		assertThat(resolver.resolve(new MockHttpServletRequest())).isEmpty();
	}

	@Test
	void emptyWhenHeaderBlank() {
		assertThat(resolver.resolve(requestWithUser("  "))).isEmpty();
	}

	@Test
	void emptyWhenUserUnknown() {
		assertThat(resolver.resolve(requestWithUser("nobody"))).isEmpty();
	}

	private static MockHttpServletRequest requestWithUser(String userId) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User-Id", userId);
		return request;
	}

}
