package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.PlanCatalog;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pro-ration calculation helper.
 *
 * <p><b>Single Responsibility:</b> this component has exactly one job —
 * calculate the pro-rated billing amount for a mid-cycle plan change.
 * All other transaction concerns (persistence, customer updates, promo logic)
 * live in {@link com.telecom.copilot_backend.service.impl.PlanTransactionServiceImpl}.
 *
 * <p>Declared as a {@code @Component} (not {@code @Service}) to signal that
 * this is a stateless calculation helper, not a business service.
 */
@Component
public class PlanTransactionService {

    /**
     * Pro-rated cost = (monthly cost / days in month) × remaining days in cycle.
     *
     * <p>Edge cases handled:
     * <ul>
     *   <li>Null {@code monthly_cost} → returns {@link BigDecimal#ZERO}</li>
     *   <li>Null {@code billing_cycle_date} → charges full monthly cost</li>
     * </ul>
     *
     * @param customer the customer (provides billing cycle start date)
     * @param plan     the target plan (provides monthly cost)
     * @return pro-rated amount for the remaining days of the current billing period
     */
    public BigDecimal calculateProRatedAmount(Customer customer, PlanCatalog plan) {
        if (plan.getMonthlyCost() == null) {
            return BigDecimal.ZERO;
        }
        LocalDate today     = LocalDate.now();
        LocalDate cycleDate = customer.getBillingCycleDate();
        if (cycleDate == null) {
            return plan.getMonthlyCost(); // no billing date — charge full month
        }
        // Determine the next billing date within the current or upcoming month
        LocalDate nextBilling = cycleDate
                .withYear(today.getYear())
                .withMonth(today.getMonthValue());
        if (!nextBilling.isAfter(today)) {
            nextBilling = nextBilling.plusMonths(1);
        }
        long daysRemaining = ChronoUnit.DAYS.between(today, nextBilling);
        long daysInMonth   = today.lengthOfMonth();
        return plan.getMonthlyCost()
                .multiply(BigDecimal.valueOf(daysRemaining))
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
    }
}
