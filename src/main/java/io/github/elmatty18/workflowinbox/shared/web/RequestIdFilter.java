package io.github.elmatty18.workflowinbox.shared.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id: the caller's {@code X-Request-Id} if it is safe to log and store,
 * otherwise a new UUID. The id is returned in the response and, with the claimed user id, put in
 * the MDC for logs and for the audit log. Runs before everything else.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-Id";

	public static final String MDC_REQUEST_ID = "requestId";

	public static final String MDC_USER_ID = "userId";

	private static final String USER_ID_HEADER = "X-User-Id";

	/** Fits audit_log.request_id and cannot inject anything into logs. */
	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

	/** Fits app_user.id; anything else is not a user id worth logging. */
	private static final Pattern SAFE_USER_ID = Pattern.compile("[A-Za-z0-9._@-]{1,50}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = request.getHeader(REQUEST_ID_HEADER);
		if (requestId == null || !SAFE_REQUEST_ID.matcher(requestId).matches()) {
			requestId = UUID.randomUUID().toString();
		}
		response.setHeader(REQUEST_ID_HEADER, requestId);
		MDC.put(MDC_REQUEST_ID, requestId);
		String userId = request.getHeader(USER_ID_HEADER);
		if (userId != null && SAFE_USER_ID.matcher(userId).matches()) {
			MDC.put(MDC_USER_ID, userId);
		}
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove(MDC_REQUEST_ID);
			MDC.remove(MDC_USER_ID);
		}
	}

}
