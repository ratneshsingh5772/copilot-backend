package com.telecom.copilot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDto {

    private Integer planId;
    private String planName;
    private String planType;
    private BigDecimal monthlyCost;
    private Integer dataLimitGb;
    private String description;
}
