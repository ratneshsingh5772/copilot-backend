package com.telecom.copilot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanTransactionDto {

    private String transactionId;
    private String customerId;
    private Integer oldPlanId;
    private String oldPlanName;
    private Integer newPlanId;
    private String newPlanName;
    private Integer promoAppliedId;
    private String promoAppliedName;
    private BigDecimal proratedBilledAmount;
    private LocalDateTime executedAt;
}

