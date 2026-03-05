package com.telecom.copilot_backend.repository;

import com.telecom.copilot_backend.entity.PlanCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanCatalogRepository extends JpaRepository<PlanCatalog, Integer> {

    List<PlanCatalog> findByPlanType(String planType);

    Optional<PlanCatalog> findByPlanName(String planName);
}

