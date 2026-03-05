package com.telecom.copilot_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Calls the Gemini Developer REST API directly using an API key.
 * This bypasses Vertex AI entirely — no billing or GCP credentials required.
 * Endpoint: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 *
 * Supports:
 * - Automatic retry with backoff on 429 Rate Limit responses
 * - Multi-model fallback (tries each configured model in order)
 * - User-friendly error messages
 */
@Slf4j
@Service
public class GeminiRestClient {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}";

    // Models tried in order; first available/non-throttled wins
    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-2.0-flash-lite",
            "gemini-2.0-flash",
            "gemini-2.0-pro-exp-02-05",
            "gemini-2.5-pro-exp-03-25"
    );

    private static final long DEFAULT_RETRY_DELAY_MS = 5000;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.0-flash-lite}")
    private String preferredModel;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiRestClient() {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends a system prompt + user message to Gemini and returns the text response.
     * On 429 rate-limit, immediately moves to the next fallback model rather than
     * waiting on the same model — keeping response times fast.
     * Only after ALL models are exhausted does it do one final short wait + retry pass.
     */
    public String generate(String systemPrompt, String userMessage) {
        String requestBodyJson = buildRequestBody(systemPrompt, userMessage);

        // Build model priority list: preferred model first, then fallbacks
        List<String> modelsToTry = buildModelList();

        Exception lastException = null;

        // ── Pass 1: try every model once, skip immediately on 429 ──────────────
        for (String currentModel : modelsToTry) {
            try {
                log.info("Calling Gemini REST API: model={}, attempt=1", currentModel);

                String responseBody = restClient.post()
                        .uri(GEMINI_API_URL, currentModel, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBodyJson)
                        .retrieve()
                        .body(String.class);

                String text = parseResponse(responseBody);
                log.info("Gemini responded successfully: model={}, responseLength={}", currentModel, text.length());
                return text;

            } catch (HttpClientErrorException e) {
                int statusCode = e.getStatusCode().value();
                lastException = e;

                if (statusCode == 429) {
                    log.warn("Rate limited on model={}. Moving to next fallback model...", currentModel);
                    // Do NOT sleep — immediately try the next model
                } else if (statusCode == 404) {
                    log.warn("Model {} not found. Trying next model...", currentModel);
                } else {
                    log.error("Gemini API error {}: model={}, body={}", statusCode, currentModel, e.getResponseBodyAsString());
                    throw new RuntimeException("Gemini API error " + statusCode + ": " + extractErrorMessage(e.getResponseBodyAsString()), e);
                }
            } catch (Exception e) {
                log.error("Unexpected error calling Gemini: model={}, error={}", currentModel, e.getMessage(), e);
                lastException = e;
            }
        }

        // ── Pass 2: all models rate-limited — wait a short time then retry once ─
        log.warn("All models rate-limited on first pass. Waiting {}ms before retry pass...", DEFAULT_RETRY_DELAY_MS);
        sleep(DEFAULT_RETRY_DELAY_MS);

        for (String currentModel : modelsToTry) {
            try {
                log.info("Calling Gemini REST API: model={}, attempt=2 (retry pass)", currentModel);

                String responseBody = restClient.post()
                        .uri(GEMINI_API_URL, currentModel, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBodyJson)
                        .retrieve()
                        .body(String.class);

                String text = parseResponse(responseBody);
                log.info("Gemini responded successfully on retry pass: model={}, responseLength={}", currentModel, text.length());
                return text;

            } catch (HttpClientErrorException e) {
                int statusCode = e.getStatusCode().value();
                lastException = e;
                if (statusCode == 429 || statusCode == 404) {
                    log.warn("Still rate-limited/unavailable on retry pass: model={}", currentModel);
                } else {
                    log.error("Gemini API error {}: model={}, body={}", statusCode, currentModel, e.getResponseBodyAsString());
                    throw new RuntimeException("Gemini API error " + statusCode + ": " + extractErrorMessage(e.getResponseBodyAsString()), e);
                }
            } catch (Exception e) {
                log.error("Unexpected error calling Gemini on retry pass: model={}, error={}", currentModel, e.getMessage(), e);
                lastException = e;
            }
        }

        // All models and retries exhausted
        String msg = lastException instanceof HttpClientErrorException hce
                ? extractErrorMessage(hce.getResponseBodyAsString())
                : (lastException != null ? lastException.getMessage() : "Unknown error");

        throw new RuntimeException(
                "The AI service is temporarily unavailable due to rate limits across all models. " +
                "Please wait a moment and try again. Detail: " + msg, lastException);
    }

    // ─── Private Helpers ────────────────────────────────────────────────────────

    private List<String> buildModelList() {
        // Start with the configured preferred model, then add fallbacks (avoiding duplicates)
        java.util.LinkedHashSet<String> models = new java.util.LinkedHashSet<>();
        models.add(preferredModel);
        models.addAll(FALLBACK_MODELS);
        return List.copyOf(models);
    }

    private String buildRequestBody(String systemPrompt, String userMessage) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();

            // System instruction
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ArrayNode systemParts = objectMapper.createArrayNode();
            systemParts.add(objectMapper.createObjectNode().put("text", systemPrompt));
            systemInstruction.set("parts", systemParts);
            requestBody.set("systemInstruction", systemInstruction);

            // User content
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");
            ArrayNode userParts = objectMapper.createArrayNode();
            userParts.add(objectMapper.createObjectNode().put("text", userMessage));
            userContent.set("parts", userParts);
            contents.add(userContent);
            requestBody.set("contents", contents);

            // Generation config
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.set("generationConfig", generationConfig);

            return requestBody.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Gemini request body", e);
        }
    }

    private String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + responseBody, e);
        }
    }


    /**
     * Extracts the human-readable error message from a Gemini error JSON body.
     */
    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("error").path("message").asText(responseBody);
        } catch (Exception ignored) {
            return responseBody;
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
