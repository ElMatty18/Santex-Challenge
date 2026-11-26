package io.github.elmatty18.workflowinbox.users;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

/**
 * Resolves the caller from the {@code X-User-Id} header against {@code app_user}. No real
 * authentication: any caller can claim any id (SPEC section 8).
 */
@Component
class HeaderCurrentUserResolver implements CurrentUserResolver {

	static final String USER_ID_HEADER = "X-User-Id";

	private final UserDirectory userDirectory;

	HeaderCurrentUserResolver(UserDirectory userDirectory) {
		this.userDirectory = userDirectory;
	}

	@Override
	public Optional<User> resolve(HttpServletRequest request) {
		String userId = request.getHeader(USER_ID_HEADER);
		if (userId == null || userId.isBlank()) {
			return Optional.empty();
		}
		return userDirectory.findById(userId.strip());
	}

}
