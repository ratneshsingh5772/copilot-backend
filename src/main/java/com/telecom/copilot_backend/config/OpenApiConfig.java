package com.telecom.copilot_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI planAdvisorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plan Advisor Copilot API")
                        .description("AI-powered telecom plan recommendation backend. " +
                                "Integrates with Ollama LLM to provide personalised " +
                                "plan recommendations and pro-rated upgrade cost calculations.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Telecom Platform Team")
                                .email("platform@telecom.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT token received from POST /api/v1/auth/login")));
    }
}

