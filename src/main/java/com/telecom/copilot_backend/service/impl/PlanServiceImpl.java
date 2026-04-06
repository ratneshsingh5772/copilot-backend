package com.telecom.copilot_backend.service.impl;

import com.telecom.copilot_backend.dto.PlanCatalogDto;
import com.telecom.copilot_backend.dto.PlanSearchCriteria;
import com.telecom.copilot_backend.entity.PlanCatalog;
import com.telecom.copilot_backend.exception.ResourceNotFoundException;
import com.telecom.copilot_backend.mapper.PlanMapper;
import com.telecom.copilot_backend.repository.PlanCatalogRepository;
import com.telecom.copilot_backend.service.IPlanService;
import com.telecom.copilot_backend.specification.PlanSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of IPlanService.
 * Handles all plan-related business logic operations with MapStruct mapping.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanServiceImpl implements IPlanService {

    private final PlanCatalogRepository planCatalogRepository;
    private final PlanMapper planMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlanCatalogDto> getAllPlans() {
        return planCatalogRepository.findAll().stream()
                .map(planMapper::toDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<PlanCatalogDto> searchPlans(PlanSearchCriteria criteria, Pageable pageable) {
        // Create specification for dynamic filtering
        PlanSpecification spec = new PlanSpecification(criteria);
        // Execute query with pagination and filtering
        Page<PlanCatalog> plans = planCatalogRepository.findAll(spec, pageable);
        // Convert entities to DTOs using mapper
        return plans.map(planMapper::toDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlanCatalogDto getPlanById(Integer planId) {
        PlanCatalog plan = findEntityById(planId);
        return planMapper.toDto(plan);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PlanCatalogDto createPlan(PlanCatalogDto dto) {
        PlanCatalog entity = planMapper.toEntity(dto);
        PlanCatalog saved = planCatalogRepository.save(entity);
        return planMapper.toDto(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PlanCatalogDto updatePlan(Integer planId, PlanCatalogDto dto) {
        PlanCatalog existing = findEntityById(planId);

        // Update fields
        existing.setPlanName(dto.getPlanName());
        existing.setPlanType(dto.getPlanType());
        existing.setDescription(dto.getDescription());
        existing.setMonthlyCost(dto.getMonthlyCost());
        existing.setDataLimitGb(dto.getDataLimitGb());
        existing.setIsActive(dto.getIsActive());

        PlanCatalog updated = planCatalogRepository.save(existing);
        return planMapper.toDto(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deletePlan(Integer planId) {
        PlanCatalog plan = findEntityById(planId);
        planCatalogRepository.delete(plan);
    }

    /**
     * Find plan entity by ID.
     *
     * @param planId the plan identifier
     * @return plan entity
     * @throws ResourceNotFoundException if plan not found
     */
    private PlanCatalog findEntityById(Integer planId) {
        return planCatalogRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "planId", String.valueOf(planId)));
    }
}

