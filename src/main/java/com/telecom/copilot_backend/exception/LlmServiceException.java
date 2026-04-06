package com.telecom.copilot_backend.exception;

/**
 * Thrown when communication with the LLM microservice fails.
 * Distinguishes LLM transport/parsing failures from general application errors.
 */
public class LlmServiceException extends RuntimeException {

    public LlmServiceException(String message) {
        super(message);
    }

    public LlmServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

