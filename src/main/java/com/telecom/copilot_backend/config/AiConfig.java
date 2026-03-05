package com.telecom.copilot_backend.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI configuration placeholder.
 * Vertex AI auto-configuration is excluded via spring.autoconfigure.exclude.
 * Gemini is called directly via {@link com.telecom.copilot_backend.service.GeminiRestClient}
 * using the Gemini Developer REST API and API key — no billing or GCP credentials required.
 */
@Configuration
public class AiConfig {
    // No beans needed — GeminiRestClient handles all AI calls directly via REST
}
