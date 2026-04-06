package com.telecom.copilot_backend.service.impl;

import com.telecom.copilot_backend.dto.PlanChangeRequest;
import com.telecom.copilot_backend.dto.PlanTransactionDto;
import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.PlanCatalog;
import com.telecom.copilot_backend.entity.PlanTransaction;
import com.telecom.copilot_backend.entity.Promotion;
import com.telecom.copilot_backend.mapper.PlanTransactionMapper;
import com.telecom.copilot_backend.repository.CustomerRepository;
import com.telecom.copilot_backend.repository.PlanTransactionRepository;
import com.telecom.copilot_backend.service.ICustomerService;
import com.telecom.copilot_backend.service.IPlanTransactionService;
import com.telecom.copilot_backend.service.PlanService;
import com.telecom.copilot_backend.service.PlanTransactionService;
import com.telecom.copilot_backend.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link IPlanTransactionService}.
 *
 * <p><b>Single Responsibility:</b> orchestrates plan-change business logic only.
 * Pro-ration maths → delegated to {@link PlanTransactionService}.
 * Entity ↔ DTO mapping  → delegated to {@link PlanTransactionMapper}.
 *
 * <p><b>Dependency Inversion:</b> consumers depend on {@link IPlanTransactionService};
 * this class is the sole {@code @Service} bean that satisfies that contract.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanTransactionServiceImpl implements IPlanTransactionService {

    private final PlanTransactionRepository planTransactionRepository;
    private final CustomerRepository        customerRepository;
    private final ICustomerService          customerService;
    private final PlanService               planService;
    private final PromotionService          promotionService;
    private final PlanTransactionService    prorationHelper;    // calculation helper
    private final PlanTransactionMapper     planTransactionMapper;

    // ─── Queries ─────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlanTransactionDto> getTransactionsForCustomer(String customerId) {
        return planTransactionRepository
                .findByCustomer_CustomerIdOrderByExecutedAtDesc(customerId)
                .stream()
                .map(planTransactionMapper::toDto)
                .collect(Collectors.toList());
    }

    // ─── Commands ────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Execution steps:
     * <ol>
     *   <li>Load customer and resolve old/new plans.</li>
     *   <li>Calculate pro-rated cost via {@link PlanTransactionService#calculateProRatedAmount}.</li>
     *   <li>Apply promo discount if {@code promoId} is supplied.</li>
     *   <li>Update {@code customer.currentPlan} and reset {@code billingCycleDate} to today.</li>
     *   <li>Explicitly save the customer record.</li>
     *   <li>Persist the immutable ledger record in {@code plan_transactions}.</li>
     * </ol>
     */
    @Override
    @Transactional
    public PlanTransactionDto executePlanChange(PlanChangeRequest request) {
        Customer   customer = customerService.findEntityByCustomerId(request.getCustomerId());
        PlanCatalog oldPlan = customer.getCurrentPlan();
        PlanCatalog newPlan = planService.findEntityById(request.getNewPlanId());

        // ── Pro-rated billing ─────────────────────────────────────────────────
        BigDecimal proRated = prorationHelper.calculateProRatedAmount(customer, newPlan);

        // ── Optional promo discount ───────────────────────────────────────────
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

        // ── Update customer record ────────────────────────────────────────────
        customer.setCurrentPlan(newPlan);
        customer.setBillingCycleDate(LocalDate.now());   // new cycle starts today
        customerRepository.save(customer);               // explicit save

        log.info("Customer {} plan updated: [{}] → [{}] | new billing cycle: {}",
                customer.getCustomerId(),
                oldPlan != null ? oldPlan.getPlanName() : "none",
                newPlan.getPlanName(),
                customer.getBillingCycleDate());

        // ── Persist ledger record ─────────────────────────────────────────────
        PlanTransaction tx = PlanTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .customer(customer)
                .oldPlan(oldPlan)
                .newPlan(newPlan)
                .promoApplied(promo)
                .proratedBilledAmount(proRated)
                .executedAt(LocalDateTime.now())
                .build();

        return planTransactionMapper.toDto(planTransactionRepository.save(tx));
    }

    // ─── Delegated helpers ────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     * Delegates directly to the stateless {@link PlanTransactionService} helper.
     */
    @Override
    public BigDecimal calculateProRatedAmount(Customer customer, PlanCatalog plan) {
        return prorationHelper.calculateProRatedAmount(customer, plan);
    }
}

