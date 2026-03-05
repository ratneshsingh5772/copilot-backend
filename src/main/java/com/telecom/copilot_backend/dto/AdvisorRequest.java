package com.telecom.copilot_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for the AI-powered plan advisor endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorRequest {

    @NotBlank(message = "Customer ID must not be blank")
    @Size(max = 50, message = "Customer ID must be at most 50 characters")
    private String customerId;

    @NotBlank(message = "Prompt must not be blank")
    @Size(max = 2000, message = "Prompt must be at most 2000 characters")
    private String prompt;
}

