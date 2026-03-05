package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.dto.ApiResponse;
import com.telecom.copilot_backend.dto.CustomerUsageDto;
import com.telecom.copilot_backend.service.CustomerUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
@Tag(name = "Customer Usage", description = "Real-time customer data and roaming consumption")
public class CustomerUsageController {

    private final CustomerUsageService customerUsageService;

    @GetMapping("/{customerId}/current")
    @Operation(summary = "Get current billing period usage for a customer")
    public ResponseEntity<ApiResponse<CustomerUsageDto>> getCurrentUsage(
            @PathVariable String customerId) {
        return ResponseEntity.ok(ApiResponse.ok(customerUsageService.getCurrentUsage(customerId)));
    }

    @GetMapping("/{customerId}/all")
    @Operation(summary = "Get all usage records for a customer")
    public ResponseEntity<ApiResponse<List<CustomerUsageDto>>> getAllUsage(
            @PathVariable String customerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                customerUsageService.getAllUsageForCustomer(customerId)));
    }

    @PostMapping
    @Operation(summary = "Create or update a usage record for a customer")
    public ResponseEntity<ApiResponse<CustomerUsageDto>> upsertUsage(
            @RequestBody CustomerUsageDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Usage record saved successfully",
                customerUsageService.upsertUsage(dto)));
    }
}

