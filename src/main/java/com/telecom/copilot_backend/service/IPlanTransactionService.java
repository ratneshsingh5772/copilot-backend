package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.PlanChangeRequest;
import com.telecom.copilot_backend.dto.PlanTransactionDto;
import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.PlanCatalog;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service contract for plan transaction operations.
 *
 * <p><b>Dependency Inversion Principle:</b> callers ({@link com.telecom.copilot_backend.controller.PlanTransactionController},
 * {@link com.telecom.copilot_backend.service.impl.AdvisorServiceImpl}) depend on this abstraction,
 * not on a concrete implementation.
 *
 * <p><b>Open/Closed Principle:</b> new transaction strategies (e.g. bulk change, scheduled change)
 * can be added by implementing this interface without touching existing callers.
 */
public interface IPlanTransactionService {

    /**
     * Returns all plan transactions for a customer, ordered by most recent first.
     *
     * @param customerId the customer identifier
     * @return list of transaction DTOs
     */
    List<PlanTransactionDto> getTransactionsForCustomer(String customerId);

    /**
     * Executes a plan change — calculates pro-rated cost, applies optional promo discount,
     * updates the customer's current plan and billing cycle, and writes the ledger record.
     *
     * @param request plan change request (customerId, newPlanId, optional promoId)
     * @return the persisted transaction DTO
     */
    PlanTransactionDto executePlanChange(PlanChangeRequest request);

    /**
     * Calculates the pro-rated charge for the remaining days of the customer's
     * current billing cycle.
     *
     * @param customer the customer (used for billing cycle date)
     * @param plan     the target plan (used for monthly cost)
     * @return pro-rated amount for the remaining billing period
     */
    BigDecimal calculateProRatedAmount(Customer customer, PlanCatalog plan);
}

