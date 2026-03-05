package com.telecom.copilot_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_usage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Integer usageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "billing_period_start")
    private LocalDate billingPeriodStart;

    @Column(name = "data_used_gb", precision = 6, scale = 2)
    private BigDecimal dataUsedGb;

    @Column(name = "roaming_used_mb", precision = 8, scale = 2)
    private BigDecimal roamingUsedMb;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}

