package com.telecom.copilot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search criteria for filtering customers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSearchCriteria {

    private String name;              // Search by customer name (contains)
    private String email;             // Search by email (contains)
    private String phoneNumber;       // Search by phone (contains)
    private Integer currentPlanId;    // Filter by plan ID
    private Integer minTenureMonths;  // Filter by minimum tenure
    private Integer maxTenureMonths;  // Filter by maximum tenure
}

