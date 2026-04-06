package com.telecom.copilot_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "plans_catalog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer planId;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    /**
     * e.g. BASE_PLAN, TRAVEL_ADD_ON, DATA_BOOSTER
     */
    @Column(name = "plan_type", length = 50)
    private String planType;

    @Column(name = "monthly_cost", precision = 10, scale = 2)
    private BigDecimal monthlyCost;

    /**
     * Data allowance in GB. Use 9999 to represent unlimited.
     */
    @Column(name = "data_limit_gb")
    private Integer dataLimitGb;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive;
}
