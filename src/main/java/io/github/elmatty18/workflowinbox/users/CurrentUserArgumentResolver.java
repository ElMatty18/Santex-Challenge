package io.github.elmatty18.workflowinbox.users;

import io.github.elmatty18.workflowinbox.shared.error.ApiException;
import io.github.elmatty18.workflowinbox.shared.error.ErrorCode;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType() == User.class;
	}

	@Override
	public User resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Object user = webRequest.getAttribute(CurrentUserInterceptor.CURRENT_USER_ATTRIBUTE,
				RequestAttributes.SCOPE_REQUEST);
		if (user == null) {
			// @CurrentUser used outside /api/**, where the interceptor does not run
			throw new ApiException(ErrorCode.UNAUTHENTICATED, "No authenticated user for this request");
		}
		return (User) user;
	}

}
