package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.PlanCatalogDto;
import com.telecom.copilot_backend.dto.PlanSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for plan management operations.
 * Defines the contract for plan-related business logic.
 */
public interface IPlanService {

    /**
     * Retrieve all plans.
     * @return list of all plans
     */
    List<PlanCatalogDto> getAllPlans();

    /**
     * Search plans with pagination and filtering.
     * @param criteria search and filter criteria
     * @param pageable pagination information (page, size, sort)
     * @return paginated list of plans matching criteria
     */
    Page<PlanCatalogDto> searchPlans(PlanSearchCriteria criteria, Pageable pageable);

    /**
     * Retrieve a plan by its ID.
     * @param planId the plan identifier
     * @return plan DTO
     * @throws com.telecom.copilot_backend.exception.ResourceNotFoundException if plan not found
     */
    PlanCatalogDto getPlanById(Integer planId);

    /**
     * Create a new plan.
     * @param dto the plan data
     * @return created plan DTO
     */
    PlanCatalogDto createPlan(PlanCatalogDto dto);

    /**
     * Update an existing plan.
     * @param planId the plan identifier
     * @param dto the updated plan data
     * @return updated plan DTO
     * @throws com.telecom.copilot_backend.exception.ResourceNotFoundException if plan not found
     */
    PlanCatalogDto updatePlan(Integer planId, PlanCatalogDto dto);

    /**
     * Delete a plan.
     * @param planId the plan identifier
     * @throws com.telecom.copilot_backend.exception.ResourceNotFoundException if plan not found
     */
    void deletePlan(Integer planId);
}

