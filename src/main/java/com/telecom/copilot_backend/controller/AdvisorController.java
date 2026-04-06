package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.dto.AdvisorRequest;
import com.telecom.copilot_backend.dto.AdvisorResponse;
import com.telecom.copilot_backend.dto.ApiResponse;
import com.telecom.copilot_backend.service.IAdvisorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/advisor")
@RequiredArgsConstructor
@Tag(name = "Plan Advisor", description = "AI-powered telecom plan recommendation endpoints")
public class AdvisorController {

    private final IAdvisorService advisorService;

    @PostMapping("/recommend")
    @Operation(
            summary = "Get AI plan recommendation",
            description = "Sends the customer's profile and a natural language prompt to the local Ollama LLM and returns a structured plan recommendation with pro-rated cost.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<AdvisorResponse>> recommend(
            @Valid @RequestBody AdvisorRequest request) {
        AdvisorResponse response = advisorService.advise(request);
        return ResponseEntity.ok(ApiResponse.ok("Recommendation generated successfully", response));
    }
}

