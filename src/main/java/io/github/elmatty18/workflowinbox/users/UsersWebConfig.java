package io.github.elmatty18.workflowinbox.users;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Identity is required only under {@code /api/**}; actuator, Swagger UI and the H2 console are
 * outside that path and therefore never see the interceptor.
 */
@Configuration
class UsersWebConfig implements WebMvcConfigurer {

	private final CurrentUserResolver currentUserResolver;

	UsersWebConfig(CurrentUserResolver currentUserResolver) {
		this.currentUserResolver = currentUserResolver;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new CurrentUserInterceptor(currentUserResolver)).addPathPatterns("/api/**");
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new CurrentUserArgumentResolver());
	}

}
