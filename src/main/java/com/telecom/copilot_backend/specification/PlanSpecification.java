package com.telecom.copilot_backend.specification;

import com.telecom.copilot_backend.dto.PlanSearchCriteria;
import com.telecom.copilot_backend.entity.PlanCatalog;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification for dynamic plan filtering.
 * Allows flexible querying based on multiple criteria.
 */
public class PlanSpecification implements Specification<PlanCatalog> {

    private final PlanSearchCriteria criteria;

    public PlanSpecification(PlanSearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<PlanCatalog> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        // Start with an empty predicate (AND condition)
        Predicate predicate = cb.conjunction();

        // Filter by plan name (case-insensitive contains)
        if (criteria.getPlanName() != null && !criteria.getPlanName().isBlank()) {
            predicate = cb.and(predicate,
                    cb.like(cb.lower(root.get("planName")), "%" + criteria.getPlanName().toLowerCase() + "%"));
        }

        // Filter by plan type (exact match)
        if (criteria.getPlanType() != null && !criteria.getPlanType().isBlank()) {
            predicate = cb.and(predicate,
                    cb.equal(root.get("planType"), criteria.getPlanType()));
        }

        // Filter by minimum monthly cost
        if (criteria.getMinMonthlyCost() != null) {
            predicate = cb.and(predicate,
                    cb.greaterThanOrEqualTo(root.get("monthlyCost"), criteria.getMinMonthlyCost()));
        }

        // Filter by maximum monthly cost
        if (criteria.getMaxMonthlyCost() != null) {
            predicate = cb.and(predicate,
                    cb.lessThanOrEqualTo(root.get("monthlyCost"), criteria.getMaxMonthlyCost()));
        }

        // Filter by minimum data limit
        if (criteria.getMinDataLimitGb() != null) {
            predicate = cb.and(predicate,
                    cb.greaterThanOrEqualTo(root.get("dataLimitGb"), criteria.getMinDataLimitGb()));
        }

        // Filter by maximum data limit
        if (criteria.getMaxDataLimitGb() != null) {
            predicate = cb.and(predicate,
                    cb.lessThanOrEqualTo(root.get("dataLimitGb"), criteria.getMaxDataLimitGb()));
        }

        // Filter by active status
        if (criteria.getIsActive() != null) {
            predicate = cb.and(predicate,
                    cb.equal(root.get("isActive"), criteria.getIsActive()));
        }

        return predicate;
    }
}

