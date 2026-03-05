package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.dto.ApiResponse;
import com.telecom.copilot_backend.dto.CopilotInteractionDto;
import com.telecom.copilot_backend.service.CopilotInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
@Tag(name = "Copilot Interactions", description = "Audit log of all AI advisor conversations")
public class CopilotInteractionController {

    private final CopilotInteractionService copilotInteractionService;

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all interaction history for a customer")
    public ResponseEntity<ApiResponse<List<CopilotInteractionDto>>> getInteractions(
            @PathVariable String customerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                copilotInteractionService.getInteractionsForCustomer(customerId)));
    }
}

