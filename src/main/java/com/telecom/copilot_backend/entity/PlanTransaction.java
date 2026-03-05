package com.telecom.copilot_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanTransaction {

    @Id
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_plan_id")
    private PlanCatalog oldPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_plan_id")
    private PlanCatalog newPlan;

    /**
     * Nullable — only set when a promotion was applied at transaction time.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_applied_id")
    private Promotion promoApplied;

    @Column(name = "prorated_billed_amount", precision = 10, scale = 2)
    private BigDecimal proratedBilledAmount;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;
}

