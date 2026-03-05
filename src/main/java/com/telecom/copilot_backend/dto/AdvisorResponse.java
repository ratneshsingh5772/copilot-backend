package com.telecom.copilot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO returned by the AI-powered plan advisor endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorResponse {

    private String interactionId;
    private String customerId;
    private String customerName;
    private String currentPlanName;
    private String identifiedIntent;
    private String recommendation;
    private BigDecimal proRatedCost;
    private String currency;
    /** Promotions the customer is currently eligible for. */
    private List<PromotionDto> eligiblePromotions;
}
