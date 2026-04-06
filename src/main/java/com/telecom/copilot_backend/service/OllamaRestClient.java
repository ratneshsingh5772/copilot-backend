package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.exception.LlmServiceException;
import com.telecom.copilot_backend.mapper.LlmResponseMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

/**
 * Ollama-backed implementation of {@link ILlmClient}.
 *
 * <p><b>Single Responsibility:</b> HTTP transport only — POST the recommendation
 * payload and GET the health status. All response parsing is delegated to
 * {@link LlmResponseMapper}.
 *
 * <p><b>Dependency Inversion:</b> callers depend on {@link ILlmClient};
 * this concrete class is wired by Spring and can be replaced by any other
 * {@link ILlmClient} implementation (e.g. OpenAI, Vertex AI) without
 * touching any caller.
 */
@Slf4j
@Service
public class OllamaRestClient implements ILlmClient {

    private static final int CONNECT_TIMEOUT_MS  = 5_000;
    private static final int GENERATE_TIMEOUT_MS = 120_000;  // LLM can be slow
    private static final int HEALTH_TIMEOUT_MS   = 3_000;

    private static final String RECOMMENDATION_PATH = "/api/v1/recommendation";
    private static final String HEALTH_PATH          = "/health";

    @Getter
    @Value("${llm.service.base-url:http://localhost:8001}")
    private String llmServiceBaseUrl;

    @Getter
    @Value("${ollama.model:llama3.2}")
    private String model;

    private final RestClient generateClient;
    private final RestClient healthClient;
    private final LlmResponseMapper responseMapper;

    /**
     * Constructor injection — {@code @Value} fields are injected by Spring after
     * construction; RestClients use only compile-time constants so they are safe
     * to build here.
     *
     * @param responseMapper mapper that handles all LLM JSON → text conversion
     */
    public OllamaRestClient(LlmResponseMapper responseMapper) {
        this.responseMapper = responseMapper;
        this.generateClient = buildRestClient(CONNECT_TIMEOUT_MS, GENERATE_TIMEOUT_MS);
        this.healthClient   = buildRestClient(CONNECT_TIMEOUT_MS, HEALTH_TIMEOUT_MS);
    }

    // ─── ILlmClient ─────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public String generate(String recommendationPayload) {
        String url = llmServiceBaseUrl + RECOMMENDATION_PATH;
        log.info("Calling LLM service: url={}", url);

        try {
            // Read as byte[] — ByteArrayHttpMessageConverter supports ALL content types
            // (including application/octet-stream which FastAPI may emit).
            // StringHttpMessageConverter in Spring 6 RestClient can reject non-text types.
            byte[] rawBytes = generateClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                    .body(recommendationPayload)
                    .retrieve()
                    .body(byte[].class);

            String responseBody = rawBytes == null ? "" : new String(rawBytes, StandardCharsets.UTF_8);
            log.debug("Raw LLM response body: {}", responseBody);

            String text = responseMapper.toAdvisorText(responseBody);
            log.info("LLM service responded: responseLength={}", text.length());
            return text;

        } catch (ResourceAccessException e) {
            log.error("Cannot reach LLM service at {}: {}", url, e.getMessage());
            throw new LlmServiceException(
                    "The LLM microservice is not reachable at " + llmServiceBaseUrl +
                    ". Please start it with: cd llm-service && uvicorn main:app --port 8001", e);
        } catch (LlmServiceException e) {
            throw e;  // already wrapped — re-throw as-is
        } catch (Exception e) {
            log.error("Unexpected error calling LLM service: {}", e.getMessage(), e);
            throw new LlmServiceException(
                    "Failed to get response from LLM service: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReachable() {
        try {
            byte[] rawBytes = healthClient.get()
                    .uri(llmServiceBaseUrl + HEALTH_PATH)
                    .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                    .retrieve()
                    .body(byte[].class);
            String responseBody = rawBytes == null ? "" : new String(rawBytes, StandardCharsets.UTF_8);
            return responseMapper.isHealthy(responseBody);
        } catch (Exception e) {
            log.warn("LLM service health check failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private static RestClient buildRestClient(int connectMs, int readMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectMs);
        factory.setReadTimeout(readMs);
        return RestClient.builder().requestFactory(factory).build();
    }
}
