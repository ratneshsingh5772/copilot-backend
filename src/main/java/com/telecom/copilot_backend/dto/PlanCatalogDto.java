package com.telecom.copilot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Plan Catalog with extended fields including active status.
 * Used for search, pagination, and advanced filtering operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCatalogDto {

    private Integer planId;
    private String planName;
    private String planType;
    private BigDecimal monthlyCost;
    private Integer dataLimitGb;
    private String description;
    private Boolean isActive;
}

