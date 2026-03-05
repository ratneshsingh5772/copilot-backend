package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.PlanDto;
import com.telecom.copilot_backend.entity.PlanCatalog;
import com.telecom.copilot_backend.exception.ResourceNotFoundException;
import com.telecom.copilot_backend.repository.PlanCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanCatalogRepository planCatalogRepository;

    public List<PlanDto> getAllPlans() {
        return planCatalogRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<PlanDto> getPlansByType(String planType) {
        return planCatalogRepository.findByPlanType(planType).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PlanDto getPlanById(Integer planId) {
        return planCatalogRepository.findById(planId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "planId", String.valueOf(planId)));
    }

    @Transactional
    public PlanDto createPlan(PlanDto dto) {
        return toDto(planCatalogRepository.save(toEntity(dto)));
    }

    @Transactional
    public PlanDto updatePlan(Integer planId, PlanDto dto) {
        PlanCatalog existing = planCatalogRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "planId", String.valueOf(planId)));
        existing.setPlanName(dto.getPlanName());
        existing.setPlanType(dto.getPlanType());
        existing.setMonthlyCost(dto.getMonthlyCost());
        existing.setDataLimitGb(dto.getDataLimitGb());
        existing.setDescription(dto.getDescription());
        return toDto(planCatalogRepository.save(existing));
    }

    @Transactional
    public void deletePlan(Integer planId) {
        PlanCatalog existing = planCatalogRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "planId", String.valueOf(planId)));
        planCatalogRepository.delete(existing);
    }

    /**
     * Builds a human-readable catalogue string injected into the Gemini system prompt.
     */
    public String buildPlanCatalogueText() {
        List<PlanCatalog> plans = planCatalogRepository.findAll();
        if (plans.isEmpty()) {
            return "No plans available in the catalogue.";
        }
        StringBuilder sb = new StringBuilder("Available Plans:\n");
        for (PlanCatalog p : plans) {
            String data = (p.getDataLimitGb() != null && p.getDataLimitGb() == 9999)
                    ? "Unlimited" : (p.getDataLimitGb() != null ? p.getDataLimitGb() + " GB" : "N/A");
            sb.append(String.format(
                    "- [ID:%d] %s | Type: %s | $%.2f/month | Data: %s | %s%n",
                    p.getPlanId(),
                    p.getPlanName(),
                    p.getPlanType(),
                    p.getMonthlyCost() != null ? p.getMonthlyCost().doubleValue() : 0.0,
                    data,
                    p.getDescription() != null ? p.getDescription() : ""
            ));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Package-internal helper
    // -------------------------------------------------------------------------

    public PlanCatalog findEntityById(Integer planId) {
        return planCatalogRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "planId", String.valueOf(planId)));
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    public PlanDto toDto(PlanCatalog p) {
        return PlanDto.builder()
                .planId(p.getPlanId())
                .planName(p.getPlanName())
                .planType(p.getPlanType())
                .monthlyCost(p.getMonthlyCost())
                .dataLimitGb(p.getDataLimitGb())
                .description(p.getDescription())
                .build();
    }

    private PlanCatalog toEntity(PlanDto dto) {
        return PlanCatalog.builder()
                .planName(dto.getPlanName())
                .planType(dto.getPlanType())
                .monthlyCost(dto.getMonthlyCost())
                .dataLimitGb(dto.getDataLimitGb())
                .description(dto.getDescription())
                .build();
    }
}
