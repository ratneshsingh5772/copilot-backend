package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.service.OllamaRestClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint.
 *
 * GET /health — returns service liveness + Ollama connectivity status.
 *
 * Example response (Ollama reachable):
 * {
 *   "status": "ok",
 *   "ollama_model": "llama3.2",
 *   "ollama_reachable": true
 * }
 *
 * Example response (Ollama not reachable):
 * {
 *   "status": "degraded",
 *   "ollama_model": "llama3.2",
 *   "ollama_reachable": false
 * }
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Service liveness and LLM microservice connectivity check")
public class HealthController {

    private final OllamaRestClient ollamaRestClient;

    @GetMapping
    @Operation(
            summary = "Health check",
            description = "Returns service status and whether the Python FastAPI LLM microservice (and Ollama behind it) is reachable."
    )
    public ResponseEntity<Map<String, Object>> health() {
        boolean llmReachable = ollamaRestClient.isReachable();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", llmReachable ? "ok" : "degraded");
        body.put("ollama_model", ollamaRestClient.getModel());
        body.put("ollama_reachable", llmReachable);
        body.put("llm_service_url", ollamaRestClient.getLlmServiceBaseUrl());
        body.put("hint", llmReachable ? "All systems operational."
                : "LLM service is not running. Start it with: cd llm-service && ./start.sh");

        return ResponseEntity.ok(body);
    }
}
