package com.telecom.copilot_backend.specification;

import com.telecom.copilot_backend.dto.CustomerSearchCriteria;
import com.telecom.copilot_backend.entity.Customer;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification for dynamic customer filtering.
 * Allows flexible querying based on multiple criteria.
 */
@RequiredArgsConstructor
@Log4j2
public class CustomerSpecification implements Specification<Customer> {

    private final CustomerSearchCriteria criteria;

    @Override
    public Predicate toPredicate(Root<Customer> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        // Start with an empty predicate (AND condition)
        Predicate predicate = cb.conjunction();

        // Filter by name (case-insensitive contains)
        if (criteria.getName() != null && !criteria.getName().isBlank()) {
            predicate = cb.and(predicate,
                    cb.like(cb.lower(root.get("name")), "%" + criteria.getName().toLowerCase() + "%"));
        }

        // Filter by email (case-insensitive contains)
        if (criteria.getEmail() != null && !criteria.getEmail().isBlank()) {
            predicate = cb.and(predicate,
                    cb.like(cb.lower(root.get("email")), "%" + criteria.getEmail().toLowerCase() + "%"));
        }

        // Filter by phone number (contains)
        if (criteria.getPhoneNumber() != null && !criteria.getPhoneNumber().isBlank()) {
            predicate = cb.and(predicate,
                    cb.like(root.get("phoneNumber"), "%" + criteria.getPhoneNumber() + "%"));
        }

        // Filter by current plan ID
        if (criteria.getCurrentPlanId() != null) {
            predicate = cb.and(predicate,
                    cb.equal(root.get("currentPlan").get("planId"), criteria.getCurrentPlanId()));
        }

        // Filter by minimum tenure months
        if (criteria.getMinTenureMonths() != null) {
            predicate = cb.and(predicate,
                    cb.greaterThanOrEqualTo(root.get("tenureMonths"), criteria.getMinTenureMonths()));
        }

        // Filter by maximum tenure months
        if (criteria.getMaxTenureMonths() != null) {
            predicate = cb.and(predicate,
                    cb.lessThanOrEqualTo(root.get("tenureMonths"), criteria.getMaxTenureMonths()));
        }

        return predicate;
    }
}

