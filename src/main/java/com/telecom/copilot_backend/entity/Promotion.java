package com.telecom.copilot_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "promotions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promo_id")
    private Integer promoId;

    @Column(name = "promo_name", nullable = false, length = 100)
    private String promoName;

    @Column(name = "discount_percentage")
    private Integer discountPercentage;

    /**
     * Minimum tenure in months required for the customer to be eligible.
     */
    @Column(name = "min_tenure_months")
    private Integer minTenureMonths;

    @Column(name = "is_active")
    private Boolean isActive;
}

