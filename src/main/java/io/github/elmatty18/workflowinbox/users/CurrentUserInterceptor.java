package io.github.elmatty18.workflowinbox.users;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;

import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Requires an identified caller on every request it is registered for ({@code /api/**}) and
 * stores it for {@link CurrentUserArgumentResolver}. Doing it here, rather than only when a
 * controller asks for {@link CurrentUser}, means no API endpoint can forget to authenticate.
 */
class CurrentUserInterceptor implements HandlerInterceptor {

	static final String CURRENT_USER_ATTRIBUTE = CurrentUserInterceptor.class.getName() + ".user";

	private final CurrentUserResolver resolver;

	CurrentUserInterceptor(CurrentUserResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		User user = resolver.resolve(request)
			.orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED,
					"A valid " + HeaderCurrentUserResolver.USER_ID_HEADER + " header is required"));
		request.setAttribute(CURRENT_USER_ATTRIBUTE, user);
		return true;
	}

}
