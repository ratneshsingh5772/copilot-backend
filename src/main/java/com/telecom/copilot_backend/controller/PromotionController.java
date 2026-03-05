package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.dto.ApiResponse;
import com.telecom.copilot_backend.dto.PromotionDto;
import com.telecom.copilot_backend.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Loyalty and promotional offer management")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @Operation(summary = "List all promotions")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getAllPromotions() {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.getAllPromotions()));
    }

    @GetMapping("/eligible/{tenureMonths}")
    @Operation(summary = "Get promotions eligible for a given customer tenure in months")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getEligible(
            @PathVariable Integer tenureMonths) {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.getEligiblePromotions(tenureMonths)));
    }

    @GetMapping("/{promoId}")
    @Operation(summary = "Get promotion by ID")
    public ResponseEntity<ApiResponse<PromotionDto>> getById(@PathVariable Integer promoId) {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.getPromotionById(promoId)));
    }

    @PostMapping
    @Operation(summary = "Create a new promotion")
    public ResponseEntity<ApiResponse<PromotionDto>> create(@RequestBody PromotionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Promotion created successfully", promotionService.createPromotion(dto)));
    }

    @PutMapping("/{promoId}")
    @Operation(summary = "Update an existing promotion")
    public ResponseEntity<ApiResponse<PromotionDto>> update(
            @PathVariable Integer promoId, @RequestBody PromotionDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Promotion updated successfully",
                promotionService.updatePromotion(promoId, dto)));
    }

    @DeleteMapping("/{promoId}")
    @Operation(summary = "Delete a promotion")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer promoId) {
        promotionService.deletePromotion(promoId);
        return ResponseEntity.ok(ApiResponse.ok("Promotion deleted successfully", null));
    }
}

