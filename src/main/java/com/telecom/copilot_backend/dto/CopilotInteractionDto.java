package com.telecom.copilot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotInteractionDto {

    private String interactionId;
    private String customerId;
    private String identifiedIntent;
    private String llmSummary;
    private LocalDateTime createdAt;
}

