package com.telecom.copilot_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI planAdvisorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plan Advisor Copilot API")
                        .description("AI-powered telecom plan recommendation backend. " +
                                "Integrates with Google Vertex AI Gemini to provide personalised " +
                                "plan recommendations and pro-rated upgrade cost calculations.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Telecom Platform Team")
                                .email("platform@telecom.com")));
    }
}

