package com.telecom.copilot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDto {

    private Integer promoId;
    private String promoName;
    private Integer discountPercentage;
    private Integer minTenureMonths;
    private Boolean isActive;
}

