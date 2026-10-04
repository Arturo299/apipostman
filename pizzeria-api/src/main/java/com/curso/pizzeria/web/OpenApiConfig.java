package com.curso.pizzeria.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI pizzeriaOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Pizzería API")
						.version("1.0.0")
						.description("Catálogo de pizzas e ingredientes. El precio de cada pizza es la suma "
								+ "del coste de sus ingredientes más un 20 % de beneficio."))
				.components(new Components().addSecuritySchemes("bearerAuth",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")));
	}
}
