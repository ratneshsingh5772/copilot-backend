package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.dto.ApiResponse;
import com.telecom.copilot_backend.dto.PlanDto;
import com.telecom.copilot_backend.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plans Catalog", description = "Telecom plan catalogue management")
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @Operation(summary = "List all plans")
    public ResponseEntity<ApiResponse<List<PlanDto>>> getAllPlans() {
        return ResponseEntity.ok(ApiResponse.ok(planService.getAllPlans()));
    }

    @GetMapping("/type/{planType}")
    @Operation(summary = "List plans by type (BASE_PLAN, TRAVEL_ADD_ON, DATA_BOOSTER)")
    public ResponseEntity<ApiResponse<List<PlanDto>>> getPlansByType(@PathVariable String planType) {
        return ResponseEntity.ok(ApiResponse.ok(planService.getPlansByType(planType)));
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get plan by ID")
    public ResponseEntity<ApiResponse<PlanDto>> getPlanById(@PathVariable Integer planId) {
        return ResponseEntity.ok(ApiResponse.ok(planService.getPlanById(planId)));
    }

    @PostMapping
    @Operation(summary = "Create a new plan")
    public ResponseEntity<ApiResponse<PlanDto>> createPlan(@RequestBody PlanDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Plan created successfully", planService.createPlan(dto)));
    }

    @PutMapping("/{planId}")
    @Operation(summary = "Update an existing plan")
    public ResponseEntity<ApiResponse<PlanDto>> updatePlan(
            @PathVariable Integer planId, @RequestBody PlanDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Plan updated successfully",
                planService.updatePlan(planId, dto)));
    }

    @DeleteMapping("/{planId}")
    @Operation(summary = "Delete a plan")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Integer planId) {
        planService.deletePlan(planId);
        return ResponseEntity.ok(ApiResponse.ok("Plan deleted successfully", null));
    }
}
