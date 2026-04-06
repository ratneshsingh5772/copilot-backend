package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.dto.ApiResponse;
import com.telecom.copilot_backend.dto.PlanCatalogDto;
import com.telecom.copilot_backend.dto.PlanDto;
import com.telecom.copilot_backend.dto.PlanSearchCriteria;
import com.telecom.copilot_backend.service.IPlanService;
import com.telecom.copilot_backend.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final IPlanService planServiceImpl;

    @GetMapping
    @Operation(summary = "List all plans")
    public ResponseEntity<ApiResponse<List<PlanDto>>> getAllPlans() {
        return ResponseEntity.ok(ApiResponse.ok(planService.getAllPlans()));
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search plans with pagination and filtering",
        security = @SecurityRequirement(name = "bearerAuth"),
        description = "Search for plans with optional filtering and pagination. " +
                "Query parameters: planName, planType, minMonthlyCost, maxMonthlyCost, " +
                "minDataLimitGb, maxDataLimitGb, isActive, page (0-indexed), size (default 10), sort (e.g., 'planName,asc')"
    )
    public ResponseEntity<ApiResponse<Page<PlanCatalogDto>>> searchPlans(
            @RequestParam(required = false) String planName,
            @RequestParam(required = false) String planType,
            @RequestParam(required = false) Double minMonthlyCost,
            @RequestParam(required = false) Double maxMonthlyCost,
            @RequestParam(required = false) Integer minDataLimitGb,
            @RequestParam(required = false) Integer maxDataLimitGb,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "planName,asc") String[] sort) {

        // Build sort order
        Sort.Order[] orders = new Sort.Order[sort.length];
        for (int i = 0; i < sort.length; i++) {
            String[] parts = sort[i].split(",");
            String field = parts[0].trim();
            String direction = parts.length > 1 ? parts[1].trim() : "asc";
            orders[i] = "desc".equalsIgnoreCase(direction)
                ? Sort.Order.desc(field)
                : Sort.Order.asc(field);
        }

        // Build pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));

        // Build search criteria
        PlanSearchCriteria criteria = PlanSearchCriteria.builder()
                .planName(planName)
                .planType(planType)
                .minMonthlyCost(minMonthlyCost)
                .maxMonthlyCost(maxMonthlyCost)
                .minDataLimitGb(minDataLimitGb)
                .maxDataLimitGb(maxDataLimitGb)
                .isActive(isActive)
                .build();

        Page<PlanCatalogDto> result = planServiceImpl.searchPlans(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
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
