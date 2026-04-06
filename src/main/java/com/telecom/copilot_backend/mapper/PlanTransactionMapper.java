package com.telecom.copilot_backend.mapper;

import com.telecom.copilot_backend.dto.PlanTransactionDto;
import com.telecom.copilot_backend.entity.PlanTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between {@link PlanTransaction} entity and
 * {@link PlanTransactionDto}.
 *
 * <p><b>Single Responsibility:</b> this interface has one job — translate
 * {@link PlanTransaction} entities into {@link PlanTransactionDto} objects.
 * All persistence and business logic remain in the service layer.
 *
 * <p>MapStruct resolves nested source paths (e.g. {@code customer.customerId})
 * at compile time and generates null-safe accessor code automatically.
 */
@Mapper(componentModel = "spring")
public interface PlanTransactionMapper {

    /**
     * Converts a {@link PlanTransaction} entity to {@link PlanTransactionDto}.
     *
     * <p>Null-safe: {@code oldPlan} and {@code promoApplied} are nullable FK relations;
     * MapStruct generates null checks for their nested fields automatically.
     *
     * @param transaction the plan transaction entity
     * @return populated DTO
     */
    @Mapping(target = "customerId",          source = "customer.customerId")
    @Mapping(target = "oldPlanId",           source = "oldPlan.planId")
    @Mapping(target = "oldPlanName",         source = "oldPlan.planName")
    @Mapping(target = "newPlanId",           source = "newPlan.planId")
    @Mapping(target = "newPlanName",         source = "newPlan.planName")
    @Mapping(target = "promoAppliedId",      source = "promoApplied.promoId")
    @Mapping(target = "promoAppliedName",    source = "promoApplied.promoName")
    @Mapping(target = "newBillingCycleDate", source = "customer.billingCycleDate")
    PlanTransactionDto toDto(PlanTransaction transaction);
}

