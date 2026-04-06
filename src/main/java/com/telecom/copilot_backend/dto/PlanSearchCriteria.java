package com.telecom.copilot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search criteria for filtering plans with pagination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanSearchCriteria {

    private String planName;           // Search by plan name (contains, case-insensitive)
    private String planType;           // Filter by plan type (exact match)
    private Double minMonthlyCost;     // Minimum monthly cost
    private Double maxMonthlyCost;     // Maximum monthly cost
    private Integer minDataLimitGb;    // Minimum data limit
    private Integer maxDataLimitGb;    // Maximum data limit
    private Boolean isActive;          // Filter by active status
}

