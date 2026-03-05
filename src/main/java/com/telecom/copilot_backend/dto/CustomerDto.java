package com.telecom.copilot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    private String customerId;
    private String name;
    private String phoneNumber;
    private Integer currentPlanId;
    private String currentPlanName;
    private Integer tenureMonths;
    private LocalDate billingCycleDate;
}
