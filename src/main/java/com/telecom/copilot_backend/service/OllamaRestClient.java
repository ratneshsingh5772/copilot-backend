package com.telecom.copilot_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Delegates AI calls to the Python FastAPI LLM microservice (localhost:8001).
 *
 * The LLM service exposes:
 *   POST /api/v1/recommendation  — plan recommendation
 *   GET  /health                 — liveness + Ollama connectivity check
 *
 * The FastAPI service is responsible for talking to Ollama directly.
 * This client simply forwards the enriched payload and returns the recommendation text.
 */
@Slf4j
@Service
public class OllamaRestClient {

    private static final int CONNECT_TIMEOUT_MS   = 5_000;
    private static final int GENERATE_TIMEOUT_MS  = 120_000;   // LLM can be slow
    private static final int HEALTH_TIMEOUT_MS    = 3_000;

    @Value("${llm.service.base-url:http://localhost:8001}")
    private String llmServiceBaseUrl;

    @Value("${ollama.model:llama3.2}")
    private String model;

    private final RestClient generateClient;
    private final RestClient healthClient;
    private final ObjectMapper objectMapper;

    public OllamaRestClient() {
        this.generateClient = buildRestClient(CONNECT_TIMEOUT_MS, GENERATE_TIMEOUT_MS);
        this.healthClient   = buildRestClient(CONNECT_TIMEOUT_MS, HEALTH_TIMEOUT_MS);
        this.objectMapper   = new ObjectMapper();
    }

    // ─── Public API ─────────────────────────────────────────────────────────────

    /**
     * Forwards a recommendation request to the FastAPI LLM service and returns
     * the model's recommendation text.
     *
     * @param recommendationPayload  JSON string matching FastAPI RecommendationRequest schema.
     * @return the personalized_message + reasoning from the LLM response.
     */
    public String generate(String recommendationPayload) {
        String url = llmServiceBaseUrl + "/api/v1/recommendation";
        log.info("Calling LLM service: url={}", url);

        try {
            String responseBody = generateClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(recommendationPayload)
                    .retrieve()
                    .body(String.class);

            String text = parseRecommendationResponse(responseBody);
            log.info("LLM service responded: responseLength={}", text.length());
            return text;

        } catch (ResourceAccessException e) {
            log.error("Cannot reach LLM service at {}: {}", url, e.getMessage());
            throw new RuntimeException(
                    "The LLM microservice is not reachable at " + llmServiceBaseUrl +
                    ". Please start it with: cd llm-service && ./start.sh", e);
        } catch (Exception e) {
            log.error("Error calling LLM service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get response from LLM service: " + e.getMessage(), e);
        }
    }

    /**
     * Health check — calls FastAPI /health which in turn checks Ollama connectivity.
     */
    public boolean isReachable() {
        try {
            String responseBody = healthClient.get()
                    .uri(llmServiceBaseUrl + "/health")
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(responseBody);
            return "ok".equals(root.path("status").asText());
        } catch (Exception e) {
            log.warn("LLM service health check failed: {}", e.getMessage());
            return false;
        }
    }

    public String getModel() {
        return model;
    }

    public String getLlmServiceBaseUrl() {
        return llmServiceBaseUrl;
    }

    // ─── Private Helpers ────────────────────────────────────────────────────────

    /**
     * Parses the FastAPI RecommendationResponse and combines reasoning +
     * personalized_message into a single advisor text block.
     */
    private String parseRecommendationResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            String planName     = root.path("recommended_plan_name").asText("");
            int    planId       = root.path("recommended_plan_id").asInt(0);
            double cost         = root.path("recommended_plan_summary").path("monthly_cost").asDouble(0);
            String reasoning    = root.path("reasoning").asText("");
            String message      = root.path("personalized_message").asText("");

            return String.format(
                    "Recommended Plan: %s (ID: %d) — $%.2f/month%n%n%s%n%n%s",
                    planName, planId, cost, reasoning, message
            ).trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM service response: " + responseBody, e);
        }
    }

    private static RestClient buildRestClient(int connectMs, int readMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectMs);
        factory.setReadTimeout(readMs);
        return RestClient.builder().requestFactory(factory).build();
    }
}
