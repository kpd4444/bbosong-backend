package com.posong.ai_laundry.global.config;

import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

	private static final String SECURITY_SCHEME_NAME = "JWT";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(apiInfo())
				.servers(serverList())
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
				.components(new Components()
						.addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
	}

	private Info apiInfo() {
		return new Info()
				.title("뽀송이 API 명세서")
				.description("뽀송이 프로젝트의 Swagger 문서입니다.")
				.version("1.0.0");
	}

	private List<Server> serverList() {
		return List.of(
				new Server().url("http://localhost:8080").description("Local"),
				new Server().url("https://api.yourdomain.com").description("Production")
		);
	}

	private SecurityScheme jwtSecurityScheme() {
		return new SecurityScheme()
				.name("Authorization")
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.in(SecurityScheme.In.HEADER);
	}
}
