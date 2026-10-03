package com.codeeditor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger metadata. Adds a bearer-token security scheme so the Swagger
 * UI ("/swagger-ui.html") exposes an "Authorize" button for testing secured
 * endpoints with a JWT.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI collaborativeEditorApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Real-Time Collaborative Code Editor API")
                        .description("Authentication, document management, and CRDT-backed collaboration.")
                        .version("2.0.0")
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
