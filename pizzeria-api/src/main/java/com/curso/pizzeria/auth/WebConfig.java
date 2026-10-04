package com.curso.pizzeria.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class WebConfig implements WebMvcConfigurer {

	private final AuthInterceptor authInterceptor;

	public WebConfig(AuthInterceptor authInterceptor) {
		this.authInterceptor = authInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor)
				.addPathPatterns("/api/**")
				.excludePathPatterns("/api/auth/login", "/api/auth/refresh");
	}
}
