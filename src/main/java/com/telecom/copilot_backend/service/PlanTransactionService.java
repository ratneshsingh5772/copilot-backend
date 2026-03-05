package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.PlanChangeRequest;
import com.telecom.copilot_backend.dto.PlanTransactionDto;
import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.PlanCatalog;
import com.telecom.copilot_backend.entity.PlanTransaction;
import com.telecom.copilot_backend.entity.Promotion;
import com.telecom.copilot_backend.repository.PlanTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanTransactionService {

    private final PlanTransactionRepository planTransactionRepository;
    private final CustomerService customerService;
    private final PlanService planService;
    private final PromotionService promotionService;

    public List<PlanTransactionDto> getTransactionsForCustomer(String customerId) {
        return planTransactionRepository
                .findByCustomer_CustomerIdOrderByExecutedAtDesc(customerId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Executes a plan change:
     * 1. Calculates the pro-rated cost for the remaining billing period.
     * 2. Applies a promo discount if supplied and eligible.
     * 3. Updates the customer's current_plan_id.
     * 4. Persists the financial ledger record in plan_transactions.
     */
    @Transactional
    public PlanTransactionDto executePlanChange(PlanChangeRequest request) {
        Customer customer = customerService.findEntityByCustomerId(request.getCustomerId());
        PlanCatalog oldPlan = customer.getCurrentPlan();
        PlanCatalog newPlan = planService.findEntityById(request.getNewPlanId());

        BigDecimal proRated = calculateProRatedAmount(customer, newPlan);

        Promotion promo = null;
        if (request.getPromoId() != null) {
            promo = promotionService.findEntityById(request.getPromoId());
            if (promo.getDiscountPercentage() != null) {
                BigDecimal discount = proRated
                        .multiply(BigDecimal.valueOf(promo.getDiscountPercentage()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                proRated = proRated.subtract(discount);
            }
        }

        // Update customer's current plan — JPA dirty-checking will flush within this @Transactional
        customer.setCurrentPlan(newPlan);

        PlanTransaction tx = PlanTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .customer(customer)
                .oldPlan(oldPlan)
                .newPlan(newPlan)
                .promoApplied(promo)
                .proratedBilledAmount(proRated)
                .executedAt(LocalDateTime.now())
                .build();

        PlanTransaction saved = planTransactionRepository.save(tx);
        return toDto(saved);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Pro-rated cost = (monthly cost / days in month) * remaining days in cycle.
     */
    public BigDecimal calculateProRatedAmount(Customer customer, PlanCatalog plan) {
        if (plan.getMonthlyCost() == null) {
            return BigDecimal.ZERO;
        }
        LocalDate today = LocalDate.now();
        LocalDate cycleDate = customer.getBillingCycleDate();
        if (cycleDate == null) {
            return plan.getMonthlyCost(); // no billing date — charge full month
        }
        // Find next billing date
        LocalDate nextBilling = cycleDate.withYear(today.getYear()).withMonth(today.getMonthValue());
        if (!nextBilling.isAfter(today)) {
            nextBilling = nextBilling.plusMonths(1);
        }
        long daysRemaining = ChronoUnit.DAYS.between(today, nextBilling);
        long daysInMonth = today.lengthOfMonth();
        return plan.getMonthlyCost()
                .multiply(BigDecimal.valueOf(daysRemaining))
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    public PlanTransactionDto toDto(PlanTransaction t) {
        return PlanTransactionDto.builder()
                .transactionId(t.getTransactionId())
                .customerId(t.getCustomer().getCustomerId())
                .oldPlanId(t.getOldPlan() != null ? t.getOldPlan().getPlanId() : null)
                .oldPlanName(t.getOldPlan() != null ? t.getOldPlan().getPlanName() : null)
                .newPlanId(t.getNewPlan().getPlanId())
                .newPlanName(t.getNewPlan().getPlanName())
                .promoAppliedId(t.getPromoApplied() != null ? t.getPromoApplied().getPromoId() : null)
                .promoAppliedName(t.getPromoApplied() != null ? t.getPromoApplied().getPromoName() : null)
                .proratedBilledAmount(t.getProratedBilledAmount())
                .executedAt(t.getExecutedAt())
                .build();
    }
}

