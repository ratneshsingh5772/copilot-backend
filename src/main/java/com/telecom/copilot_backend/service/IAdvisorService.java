package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.AdvisorRequest;
import com.telecom.copilot_backend.dto.AdvisorResponse;

/**
 * Service interface for AI-powered plan recommendation.
 * Provides intelligent plan advice based on customer profiles and natural language queries.
 */
public interface IAdvisorService {

    /**
     * Generate AI-powered plan recommendation for a customer.
     * Analyzes customer profile, usage patterns, and tenure to recommend suitable plans.
     * Integrates with Ollama LLM for natural language processing.
     *
     * @param request advisor request containing customer ID and natural language prompt
     * @return advisor response with recommended plan, cost, and eligible promotions
     * @throws com.telecom.copilot_backend.exception.ResourceNotFoundException if customer not found
     * @throws IllegalArgumentException if request validation fails
     */
    AdvisorResponse advise(AdvisorRequest request);
}

