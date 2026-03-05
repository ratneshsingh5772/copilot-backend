package com.telecom.copilot_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload to execute a plan change for a customer.
 * An optional promo ID can be supplied to apply a discount.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanChangeRequest {

    @NotBlank(message = "Customer ID must not be blank")
    private String customerId;

    @NotNull(message = "New plan ID must not be null")
    private Integer newPlanId;

    /** Optional: promo to apply at execution time. */
    private Integer promoId;
}

