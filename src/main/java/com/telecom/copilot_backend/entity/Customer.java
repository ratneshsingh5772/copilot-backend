package com.telecom.copilot_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @Column(name = "customer_id", length = 50)
    @UuidGenerator
    private String customerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(nullable = false, unique = true, length = 100)
    @Email(message = "Email should be valid")
    private String email;

    @Column(length = 255)
    @NotBlank(message = "Password cannot be blank")
    private String password;

    /**
     * FK → plans_catalog.plan_id — the customer's currently subscribed plan.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_plan_id")
    private PlanCatalog currentPlan;

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "billing_cycle_date")
    private LocalDate billingCycleDate;
}
