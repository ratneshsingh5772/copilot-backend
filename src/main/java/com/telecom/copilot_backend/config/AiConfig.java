package com.telecom.copilot_backend.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI configuration placeholder.
 * Vertex AI auto-configuration is excluded via spring.autoconfigure.exclude.
 * LLM inference is handled locally via {@link com.telecom.copilot_backend.service.OllamaRestClient}
 * which calls the Ollama REST API at http://localhost:11434 — no cloud credentials required.
 */
@Configuration
public class AiConfig {
    // No beans needed — OllamaRestClient handles all AI calls directly via REST
}
