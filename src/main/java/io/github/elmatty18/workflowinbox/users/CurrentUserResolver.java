package io.github.elmatty18.workflowinbox.users;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Identifies the caller of a request. The current implementation trusts the {@code X-User-Id}
 * header ({@link HeaderCurrentUserResolver}); a JWT-based implementation can replace it without
 * touching controllers or services.
 */
public interface CurrentUserResolver {

	/**
	 * @return the caller, or empty if the request carries no valid identity
	 */
	Optional<User> resolve(HttpServletRequest request);

}
