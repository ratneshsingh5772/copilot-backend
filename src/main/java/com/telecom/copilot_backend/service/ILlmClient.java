package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.exception.LlmServiceException;

/**
 * Abstraction over any LLM HTTP client.
 *
 * <p>Follows the <b>Dependency Inversion Principle</b> — callers depend on this
 * interface, not on a concrete class such as {@link OllamaRestClient}.
 *
 * <p>Follows the <b>Open/Closed Principle</b> — new LLM providers (e.g. OpenAI,
 * Vertex AI) can be added by implementing this interface without changing any
 * existing caller.
 *
 * <p>Follows <b>Interface Segregation</b> — the interface is focused solely on
 * LLM communication; it does not expose HTTP or infrastructure internals.
 */
public interface ILlmClient {

    /**
     * Sends a structured JSON payload to the LLM service and returns
     * a formatted recommendation text.
     *
     * @param recommendationPayload JSON string matching the LLM service's
     *                              {@code RecommendationRequest} schema
     * @return formatted advisor recommendation text
     * @throws LlmServiceException if the service is unreachable or returns an error
     */
    String generate(String recommendationPayload);

    /**
     * Checks whether the LLM service is reachable and healthy.
     *
     * @return {@code true} if the service responded with status {@code "ok"}
     */
    boolean isReachable();

    /**
     * Returns the configured LLM model name (e.g. {@code llama3.2}).
     */
    String getModel();

    /**
     * Returns the base URL of the configured LLM microservice.
     */
    String getLlmServiceBaseUrl();
}

