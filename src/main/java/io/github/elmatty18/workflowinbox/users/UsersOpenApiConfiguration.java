package io.github.elmatty18.workflowinbox.users;

import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documents identity the way it works: the {@code X-User-Id} header on every API operation. The
 * {@link CurrentUser} controller parameter is resolved from that header, so it is hidden.
 */
@Configuration(proxyBeanMethods = false)
class UsersOpenApiConfiguration {

	UsersOpenApiConfiguration() {
		SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
	}

	@Bean
	OpenApiCustomizer userIdHeader() {
		return openApi -> openApi.getPaths()
			.values()
			.forEach(path -> path.readOperations()
				.forEach(operation -> operation.addParametersItem(new HeaderParameter()
					.name(HeaderCurrentUserResolver.USER_ID_HEADER)
					.required(true)
					.description("Id of the calling user (no real authentication). "
							+ "Seeded: admin, ana (REQUESTER), lucia and martin (REVIEWER).")
					.schema(new StringSchema().example("ana")))));
	}

}
