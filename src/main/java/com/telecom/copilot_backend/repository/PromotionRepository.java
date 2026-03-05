package com.telecom.copilot_backend.repository;

import com.telecom.copilot_backend.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    /**
     * Returns all active promotions where the customer's tenure qualifies.
     */
    List<Promotion> findByIsActiveTrueAndMinTenureMonthsLessThanEqual(Integer tenureMonths);
}

